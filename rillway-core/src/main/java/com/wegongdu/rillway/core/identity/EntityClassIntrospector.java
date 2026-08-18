package com.wegongdu.rillway.core.identity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Introspects JavaBean / Record / Entity classes to extract table names, column mappings,
 * field descriptions (Swagger/OpenAPI @Schema, @ApiModelProperty), and schema metadata.
 */
public final class EntityClassIntrospector {

    private EntityClassIntrospector() {}

    public record FieldMeta(
            String fieldName,
            String columnName,
            Class<?> javaType,
            String description
    ) {}

    public record TableMeta(
            Class<?> entityClass,
            String tableName,
            String idColumn,
            String nameColumn,
            String deptColumn,
            String leaderColumn,
            String codeColumn,
            List<FieldMeta> fields
    ) {
        public String toPromptDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append("实体类: `").append(entityClass.getSimpleName()).append("` (表名: `").append(tableName).append("`)\n");
            sb.append("包含字段列表:\n");
            for (FieldMeta f : fields) {
                sb.append("  - `").append(f.fieldName()).append("` (列名: `").append(f.columnName())
                        .append("`, 类型: ").append(f.javaType().getSimpleName());
                if (f.description() != null && !f.description().isBlank()) {
                    sb.append(", 含义: \"").append(f.description()).append("\"");
                }
                sb.append(")\n");
            }
            return sb.toString();
        }
    }

    public static TableMeta introspect(Class<?> clazz) {
        if (clazz == null) return null;

        String tableName = resolveTableName(clazz);
        List<FieldMeta> fieldMetas = new ArrayList<>();
        String idCol = "id";
        String nameCol = "name";
        String deptCol = "dept_id";
        String leaderCol = "leader_user_id";
        String codeCol = "code";

        for (Field field : getAllFields(clazz)) {
            String fieldName = field.getName();
            if ("serialVersionUID".equals(fieldName)) continue;

            String colName = camelToSnake(fieldName);
            String desc = resolveFieldDescription(field, clazz);

            // 识别 MyBatis-Plus @TableField / @TableId
            String explicitCol = resolveAnnotationValue(field, "com.baomidou.mybatisplus.annotation.TableField", "value");
            if (explicitCol == null || explicitCol.isBlank()) {
                explicitCol = resolveAnnotationValue(field, "com.baomidou.mybatisplus.annotation.TableId", "value");
            }
            if (explicitCol != null && !explicitCol.isBlank()) {
                colName = explicitCol;
            }

            fieldMetas.add(new FieldMeta(fieldName, colName, field.getType(), desc));

            // 自动推导关键列
            if ("id".equalsIgnoreCase(fieldName) || "userId".equalsIgnoreCase(fieldName)) {
                idCol = colName;
            } else if ("nickname".equalsIgnoreCase(fieldName) || "username".equalsIgnoreCase(fieldName) || "name".equalsIgnoreCase(fieldName)) {
                nameCol = colName;
            } else if ("deptId".equalsIgnoreCase(fieldName) || "departmentId".equalsIgnoreCase(fieldName)) {
                deptCol = colName;
            } else if ("leaderUserId".equalsIgnoreCase(fieldName) || "leaderId".equalsIgnoreCase(fieldName) || "managerId".equalsIgnoreCase(fieldName)) {
                leaderCol = colName;
            } else if ("code".equalsIgnoreCase(fieldName)) {
                codeCol = colName;
            }
        }

        return new TableMeta(clazz, tableName, idCol, nameCol, deptCol, leaderCol, codeCol, Collections.unmodifiableList(fieldMetas));
    }

    private static String resolveTableName(Class<?> clazz) {
        // 1. MyBatis-Plus @TableName
        String mpTable = resolveClassAnnotationValue(clazz, "com.baomidou.mybatisplus.annotation.TableName", "value");
        if (mpTable != null && !mpTable.isBlank()) return mpTable;

        // 2. JPA @Table(name = "...")
        String jpaTable = resolveClassAnnotationValue(clazz, "jakarta.persistence.Table", "name");
        if (jpaTable == null || jpaTable.isBlank()) {
            jpaTable = resolveClassAnnotationValue(clazz, "javax.persistence.Table", "name");
        }
        if (jpaTable != null && !jpaTable.isBlank()) return jpaTable;

        // 3. 类名驼峰转下划线
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("DO") || simpleName.endsWith("Entity") || simpleName.endsWith("PO")) {
            simpleName = simpleName.replaceAll("(DO|Entity|PO)$", "");
        }
        return camelToSnake(simpleName);
    }

    private static String resolveFieldDescription(Field field, Class<?> clazz) {
        // 1. Swagger / OpenAPI @Schema(description = "...")
        String schemaDesc = resolveAnnotationValue(field, "io.swagger.v3.oas.annotations.media.Schema", "description");
        if (schemaDesc != null && !schemaDesc.isBlank()) return schemaDesc;

        // 2. Swagger 2 @ApiModelProperty(value = "...")
        String apiDesc = resolveAnnotationValue(field, "io.swagger.annotations.ApiModelProperty", "value");
        if (apiDesc != null && !apiDesc.isBlank()) return apiDesc;

        // 3. Getter 方法上的注解
        try {
            String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            Method getter = clazz.getMethod(getterName);
            String getterDesc = resolveAnnotationValueOnMethod(getter, "io.swagger.v3.oas.annotations.media.Schema", "description");
            if (getterDesc != null && !getterDesc.isBlank()) return getterDesc;
        } catch (Exception ignored) {}

        return null;
    }

    private static String resolveClassAnnotationValue(Class<?> clazz, String annotationClassName, String attributeName) {
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationClassName)) {
                try {
                    Method m = ann.annotationType().getMethod(attributeName);
                    Object val = m.invoke(ann);
                    return val != null ? val.toString() : null;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String resolveAnnotationValue(Field field, String annotationClassName, String attributeName) {
        for (Annotation ann : field.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationClassName)) {
                try {
                    Method m = ann.annotationType().getMethod(attributeName);
                    Object val = m.invoke(ann);
                    return val != null ? val.toString() : null;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String resolveAnnotationValueOnMethod(Method method, String annotationClassName, String attributeName) {
        for (Annotation ann : method.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationClassName)) {
                try {
                    Method m = ann.annotationType().getMethod(attributeName);
                    Object val = m.invoke(ann);
                    return val != null ? val.toString() : null;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String camelToSnake(String str) {
        if (str == null || str.isBlank()) return str;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
