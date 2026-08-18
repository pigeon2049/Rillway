package com.wegongdu.rillway.core.util;

import com.wegongdu.rillway.core.annotation.EntityId;
import com.wegongdu.rillway.core.annotation.ProcessInitiator;
import com.wegongdu.rillway.core.annotation.RillwayEntity;
import com.wegongdu.rillway.core.context.ProcessContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * Utility for extracting workflow metadata (businessType, entityId, initiator, variables)
 * from arbitrary Java Beans, POJOs, Records, and Maps with zero mandatory framework dependencies.
 */
public final class EntityBeanResolver {

    private static final List<String> COMMON_ID_NAMES = List.of(
            "id", "orderid", "orderno", "billno", "code", "businesskey", "entityid", "key", "billid"
    );

    private static final List<String> COMMON_INITIATOR_NAMES = List.of(
            "initiator", "userid", "creator", "applicant", "createby", "author", "username", "user",
            "creatoruserid", "creatorid", "createbyid", "createuserid", "applicantid", "applyuserid", "applicantuserid"
    );

    private static final Set<String> SENSITIVE_PROPERTY_NAMES = Set.of(
            "password", "passwd", "secret", "token", "accesstoken", "refreshtoken",
            "credential", "privatekey", "apisecret", "authheader"
    );

    private EntityBeanResolver() {}

    /**
     * Checks whether an object is an invalid/illegal entity parameter (e.g. primitive, string, collection).
     */
    public static boolean isInvalidEntity(Object bean) {
        if (bean == null) return true;
        return bean instanceof CharSequence
                || bean instanceof Number
                || bean instanceof Boolean
                || bean instanceof Character
                || bean instanceof Iterable<?>
                || bean instanceof Map<?, ?>;
    }

    /**
     * Resolves the business type name from a class.
     */
    public static String resolveBusinessType(Class<?> clazz) {
        if (clazz == null) return null;

        // 1. Check @RillwayEntity
        RillwayEntity rillwayEntity = clazz.getAnnotation(RillwayEntity.class);
        if (rillwayEntity != null) {
            if (!rillwayEntity.value().isBlank()) return rillwayEntity.value().trim();
            if (!rillwayEntity.businessType().isBlank()) return rillwayEntity.businessType().trim();
        }

        // 2. Check MyBatis-Plus @TableName or JPA @Table via reflection
        for (Annotation ann : clazz.getAnnotations()) {
            String annName = ann.annotationType().getSimpleName();
            if ("TableName".equals(annName) || "Table".equals(annName)) {
                String tableName = readStringAttribute(ann, "value", "name");
                if (tableName != null && !tableName.isBlank()) {
                    return tableName.trim();
                }
            }
        }

        // 3. Fallback to camelToSnake(SimpleClassName), stripping common suffixes
        String simpleName = clazz.getSimpleName();
        for (String suffix : List.of("Bean", "DTO", "Dto", "VO", "Vo", "Entity", "Form", "Req", "Request", "Model")) {
            if (simpleName.endsWith(suffix) && simpleName.length() > suffix.length()) {
                simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
                break;
            }
        }
        return camelToSnake(simpleName);
    }

    /**
     * Resolves the primary key / entity ID from a bean instance.
     */
    public static String resolveEntityId(Object bean) {
        if (bean == null) return null;
        if (bean instanceof Map<?, ?> map) {
            for (String idName : COMMON_ID_NAMES) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && matchName(idName, entry.getKey().toString())) {
                        return entry.getValue() != null ? entry.getValue().toString() : null;
                    }
                }
            }
            return null;
        }

        Class<?> clazz = bean.getClass();

        // 1. Check @RillwayEntity(idField = "...")
        RillwayEntity rillwayEntity = clazz.getAnnotation(RillwayEntity.class);
        if (rillwayEntity != null && !rillwayEntity.idField().isBlank()) {
            Object val = readProperty(bean, rillwayEntity.idField().trim());
            if (val != null) return val.toString();
        }

        // 2. Check annotated fields/methods (@EntityId, @TableId, @Id)
        for (Field f : getAllFields(clazz)) {
            if (hasIdAnnotation(f) && !isIgnored(f)) {
                Object val = readFieldValue(bean, f);
                if (val != null) return val.toString();
            }
        }
        for (Method m : clazz.getMethods()) {
            if (hasIdAnnotation(m) && m.getParameterCount() == 0 && !isIgnored(m)) {
                Object val = invokeMethod(bean, m);
                if (val != null) return val.toString();
            }
        }

        // 3. Check common field/property names
        for (Field f : getAllFields(clazz)) {
            if (!Modifier.isStatic(f.getModifiers()) && !isIgnored(f)) {
                for (String idName : COMMON_ID_NAMES) {
                    if (matchName(idName, f.getName())) {
                        Object val = readFieldValue(bean, f);
                        if (val != null) return val.toString();
                    }
                }
            }
        }
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) && !isIgnored(m)) {
                String propName = extractPropName(m.getName());
                if (propName != null) {
                    for (String idName : COMMON_ID_NAMES) {
                        if (matchName(idName, propName)) {
                            Object val = invokeMethod(bean, m);
                            if (val != null) return val.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resolves initiator from a bean instance.
     */
    public static String resolveInitiator(Object bean) {
        if (bean == null) return null;
        if (bean instanceof Map<?, ?> map) {
            for (String initName : COMMON_INITIATOR_NAMES) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && matchName(initName, entry.getKey().toString())) {
                        return entry.getValue() != null ? entry.getValue().toString() : null;
                    }
                }
            }
            return null;
        }

        Class<?> clazz = bean.getClass();

        // 1. Check @RillwayEntity(initiatorField = "...")
        RillwayEntity rillwayEntity = clazz.getAnnotation(RillwayEntity.class);
        if (rillwayEntity != null && !rillwayEntity.initiatorField().isBlank()) {
            Object val = readProperty(bean, rillwayEntity.initiatorField().trim());
            if (val != null) return val.toString();
        }

        // 2. Check annotated fields/methods (@ProcessInitiator)
        for (Field f : getAllFields(clazz)) {
            if (f.isAnnotationPresent(ProcessInitiator.class) && !isIgnored(f)) {
                Object val = readFieldValue(bean, f);
                if (val != null) return val.toString();
            }
        }
        for (Method m : clazz.getMethods()) {
            if (m.isAnnotationPresent(ProcessInitiator.class) && m.getParameterCount() == 0 && !isIgnored(m)) {
                Object val = invokeMethod(bean, m);
                if (val != null) return val.toString();
            }
        }

        // 3. Check common field/property names
        for (Field f : getAllFields(clazz)) {
            if (!Modifier.isStatic(f.getModifiers()) && !isIgnored(f)) {
                for (String initName : COMMON_INITIATOR_NAMES) {
                    if (matchName(initName, f.getName())) {
                        Object val = readFieldValue(bean, f);
                        if (val != null) return val.toString();
                    }
                }
            }
        }
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) && !isIgnored(m)) {
                String propName = extractPropName(m.getName());
                if (propName != null) {
                    for (String initName : COMMON_INITIATOR_NAMES) {
                        if (matchName(initName, propName)) {
                            Object val = invokeMethod(bean, m);
                            if (val != null) return val.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extracts all properties of a Bean/Record/Map as a key-value Map, skipping ignored or sensitive fields.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractVariables(Object bean) {
        if (bean == null) return Collections.emptyMap();
        if (bean instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    String k = e.getKey().toString();
                    if (!isSensitiveName(k)) {
                        result.put(k, e.getValue());
                    }
                }
            }
            return result;
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        Class<?> clazz = bean.getClass();

        // 1. Java 21 Record support
        if (clazz.isRecord()) {
            for (RecordComponent rc : clazz.getRecordComponents()) {
                if (isIgnored(rc)) continue;
                String propName = resolveVariableName(rc.getName(), rc.getAnnotation(com.wegongdu.rillway.core.annotation.ProcessVariable.class));
                if (isSensitiveName(propName)) continue;

                try {
                    Object val = rc.getAccessor().invoke(bean);
                    if (val != null && !isHeavyType(val)) {
                        variables.put(propName, val);
                    }
                } catch (Exception ignored) {}
            }
            return variables;
        }

        // 2. Standard JavaBean Getters
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers())) {
                if (isIgnored(method)) continue;

                String name = method.getName();
                String propName = null;
                if (name.startsWith("get") && name.length() > 3 && !"getClass".equals(name)) {
                    propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2) {
                    propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }

                if (propName != null) {
                    propName = resolveVariableName(propName, method.getAnnotation(com.wegongdu.rillway.core.annotation.ProcessVariable.class));
                    if (isSensitiveName(propName)) continue;

                    try {
                        Object val = method.invoke(bean);
                        if (val != null && !isHeavyType(val)) {
                            variables.put(propName, val);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // 3. Fields fallback for non-getter properties
        for (Field f : getAllFields(clazz)) {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())) {
                if (isIgnored(f)) continue;

                String propName = resolveVariableName(f.getName(), f.getAnnotation(com.wegongdu.rillway.core.annotation.ProcessVariable.class));
                if (isSensitiveName(propName)) continue;

                if (!variables.containsKey(propName)) {
                    Object val = readFieldValue(bean, f);
                    if (val != null && !isHeavyType(val)) {
                        variables.put(propName, val);
                    }
                }
            }
        }

        return variables;
    }

    private static boolean isIgnored(java.lang.reflect.AnnotatedElement elem) {
        if (elem == null) return false;
        if (elem.isAnnotationPresent(com.wegongdu.rillway.core.annotation.ProcessIgnore.class)) return true;
        for (Annotation ann : elem.getAnnotations()) {
            String name = ann.annotationType().getSimpleName();
            if ("Transient".equals(name) || "JsonIgnore".equals(name)) return true;
        }
        return false;
    }

    private static boolean isSensitiveName(String propName) {
        if (propName == null) return false;
        String lower = propName.toLowerCase().replace("_", "").replace("-", "");
        for (String sensitive : SENSITIVE_PROPERTY_NAMES) {
            if (lower.contains(sensitive)) return true;
        }
        return false;
    }

    private static boolean isHeavyType(Object val) {
        if (val == null) return false;
        if (val instanceof byte[]) return true;
        if (val instanceof java.io.InputStream || val instanceof java.io.OutputStream) return true;
        if (val instanceof java.io.File || val instanceof java.nio.file.Path) return true;
        return false;
    }

    private static String resolveVariableName(String defaultName, com.wegongdu.rillway.core.annotation.ProcessVariable ann) {
        if (ann != null && !ann.value().isBlank()) {
            return ann.value().trim();
        }
        return defaultName;
    }

    /**
     * Converts a Bean/Record/Map into a ProcessContext automatically.
     */
    public static ProcessContext resolveContext(Object bean) {
        if (bean == null) return ProcessContext.empty();
        if (bean instanceof ProcessContext ctx) return ctx;

        String initiator = resolveInitiator(bean);
        Map<String, Object> vars = extractVariables(bean);
        return ProcessContext.builder()
                .initiator(initiator != null ? initiator : "default_user")
                .variables(vars)
                .build();
    }

    /**
     * Converts a Bean/Record/Map into a ProcessContext with explicit initiator.
     */
    public static ProcessContext resolveContext(String initiator, Object bean) {
        if (bean == null) {
            return ProcessContext.builder().initiator(initiator).build();
        }
        if (bean instanceof ProcessContext ctx) {
            return (initiator != null && !initiator.isBlank())
                    ? ProcessContext.builder().initiator(initiator).variables(ctx.variables()).build()
                    : ctx;
        }

        String targetInitiator = (initiator != null && !initiator.isBlank()) ? initiator : resolveInitiator(bean);
        Map<String, Object> vars = extractVariables(bean);
        return ProcessContext.builder()
                .initiator(targetInitiator != null ? targetInitiator : "default_user")
                .variables(vars)
                .build();
    }

    public static String camelToSnake(String str) {
        if (str == null || str.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean hasIdAnnotation(java.lang.reflect.AccessibleObject obj) {
        if (obj.isAnnotationPresent(EntityId.class)) return true;
        for (Annotation ann : obj.getAnnotations()) {
            String name = ann.annotationType().getSimpleName();
            if ("TableId".equals(name) || "Id".equals(name)) return true;
        }
        return false;
    }

    private static Object readProperty(Object bean, String propName) {
        Class<?> clazz = bean.getClass();
        String getterName = "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
        try {
            Method m = clazz.getMethod(getterName);
            return m.invoke(bean);
        } catch (Exception ignored) {}

        String isName = "is" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
        try {
            Method m = clazz.getMethod(isName);
            return m.invoke(bean);
        } catch (Exception ignored) {}

        try {
            Field f = findField(clazz, propName);
            if (f != null) {
                return readFieldValue(bean, f);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static Object readFieldValue(Object bean, Field f) {
        try {
            f.setAccessible(true);
            return f.get(bean);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invokeMethod(Object bean, Method m) {
        try {
            m.setAccessible(true);
            return m.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(fieldName)) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean matchName(String targetName, String candidate) {
        if (targetName == null || candidate == null) return false;
        String n1 = targetName.toLowerCase().replace("_", "").replace("-", "");
        String n2 = candidate.toLowerCase().replace("_", "").replace("-", "");
        return n1.equals(n2);
    }

    private static String extractPropName(String methodName) {
        if (methodName == null) return null;
        if (methodName.startsWith("get") && methodName.length() > 3 && !"getClass".equals(methodName)) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }

    private static String readStringAttribute(Annotation ann, String... attrNames) {
        for (String attr : attrNames) {
            try {
                Method m = ann.annotationType().getMethod(attr);
                Object val = m.invoke(ann);
                if (val instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
