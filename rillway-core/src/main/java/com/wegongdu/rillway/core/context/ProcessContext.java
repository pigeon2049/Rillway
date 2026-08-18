package com.wegongdu.rillway.core.context;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable execution context carrying process variables, initiator, and metadata.
 */
public final class ProcessContext implements Serializable {

    private final String initiator;
    private final Map<String, Object> variables;

    private ProcessContext(String initiator, Map<String, Object> variables) {
        this.initiator = initiator;
        this.variables = Map.copyOf(variables != null ? variables : Collections.emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProcessContext empty() {
        return new Builder().build();
    }

    /**
     * Converts an arbitrary JavaBean / Record / Map into a ProcessContext,
     * automatically discovering initiator and all form variables.
     */
    public static ProcessContext from(Object bean) {
        return com.wegongdu.rillway.core.util.EntityBeanResolver.resolveContext(bean);
    }

    /**
     * Converts an arbitrary JavaBean / Record / Map into a ProcessContext with explicit initiator.
     */
    public static ProcessContext from(String initiator, Object bean) {
        return com.wegongdu.rillway.core.util.EntityBeanResolver.resolveContext(initiator, bean);
    }

    /**
     * Alias for from(bean).
     */
    public static ProcessContext of(Object bean) {
        return from(bean);
    }

    /**
     * Alias for from(initiator, bean).
     */
    public static ProcessContext of(String initiator, Object bean) {
        return from(initiator, bean);
    }

    public String initiator() {
        return initiator;
    }

    /**
     * Returns the initiator parsed as a Long ID (e.g. Snowflake / database sequence ID).
     */
    public Long initiatorLong() {
        if (initiator == null || initiator.isBlank()) return null;
        try {
            return Long.parseLong(initiator.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the initiator converted to the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T initiator(Class<T> type) {
        if (initiator == null) return null;
        if (type.isInstance(initiator)) {
            return (T) initiator;
        }
        if (type == Long.class || type == long.class) {
            return (T) initiatorLong();
        }
        if (type == Integer.class || type == int.class) {
            try {
                return (T) Integer.valueOf(initiator.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Map<String, Object> variables() {
        return variables;
    }

    public Object get(String key) {
        return variables.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object val = variables.get(key);
        if (val == null) {
            return Optional.empty();
        }
        if (type.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    public String getString(String key) {
        Object val = variables.get(key);
        return val != null ? val.toString() : null;
    }

    public BigDecimal getDecimal(String key) {
        Object val = variables.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return new BigDecimal(val.toString());
    }

    public Boolean getBoolean(String key) {
        Object val = variables.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(val.toString());
    }

    public Long getLong(String key) {
        Object val = variables.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(val.toString());
    }

    public Integer getInteger(String key) {
        Object val = variables.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Number num) {
            return num.intValue();
        }
        return Integer.parseInt(val.toString());
    }

    public ProcessContext withVariable(String key, Object value) {
        Map<String, Object> newVars = new HashMap<>(this.variables);
        newVars.put(key, value);
        return new ProcessContext(this.initiator, newVars);
    }

    public ProcessContext withVariables(Map<String, Object> additionalVariables) {
        if (additionalVariables == null || additionalVariables.isEmpty()) {
            return this;
        }
        Map<String, Object> newVars = new HashMap<>(this.variables);
        newVars.putAll(additionalVariables);
        return new ProcessContext(this.initiator, newVars);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessContext that = (ProcessContext) o;
        return Objects.equals(initiator, that.initiator) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiator, variables);
    }

    @Override
    public String toString() {
        return "ProcessContext{" +
                "initiator='" + initiator + '\'' +
                ", variables=" + variables +
                '}';
    }

    public static final class Builder {
        private String initiator;
        private final Map<String, Object> variables = new HashMap<>();

        public Builder initiator(String initiator) {
            this.initiator = initiator;
            return this;
        }

        public Builder variable(String key, Object value) {
            if (key != null && value != null) {
                this.variables.put(key, value);
            }
            return this;
        }

        public Builder variables(Map<String, Object> vars) {
            if (vars != null) {
                this.variables.putAll(vars);
            }
            return this;
        }

        public ProcessContext build() {
            return new ProcessContext(initiator, variables);
        }
    }
}
