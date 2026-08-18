package com.wegongdu.rillway.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Rillway.
 */
@ConfigurationProperties(prefix = "rillway")
public class RillwayProperties {

    /**
     * Whether to enable Rillway workflow runtime.
     */
    private boolean enabled = true;

    /**
     * Audit configuration.
     */
    private Audit audit = new Audit();

    /**
     * Engine configuration.
     */
    private Engine engine = new Engine();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public static class Audit {
        private boolean enabled = true;
        private String sink = "in-memory"; // in-memory, no-op

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSink() {
            return sink;
        }

        public void setSink(String sink) {
            this.sink = sink;
        }
    }

    public static class Engine {
        private boolean strictAuthorityCheck = true;

        public boolean isStrictAuthorityCheck() {
            return strictAuthorityCheck;
        }

        public void setStrictAuthorityCheck(boolean strictAuthorityCheck) {
            this.strictAuthorityCheck = strictAuthorityCheck;
        }
    }
}
