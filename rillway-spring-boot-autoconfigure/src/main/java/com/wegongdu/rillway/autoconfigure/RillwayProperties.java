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

    /**
     * AI & LLM configuration.
     */
    private Ai ai = new Ai();

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

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
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

    public static class Ai {
        private OpenAi openai = new OpenAi();

        public OpenAi getOpenai() {
            return openai;
        }

        public void setOpenai(OpenAi openai) {
            this.openai = openai;
        }
    }

    public static class OpenAi {
        private boolean enabled = false;
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o-mini";
        private Double temperature = 0.1;
        private Integer timeoutSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
