package com.wegongdu.rillway.ai.config;

import java.io.Serializable;
import java.time.Instant;

/**
 * AI Model Configuration entity for OpenAI-compatible providers.
 */
public record AiModelConfig(
        String id,
        String providerName,
        String baseUrl,
        String apiKey,
        String modelName,
        Double temperature,
        Integer timeoutSeconds,
        boolean isDefault,
        boolean enabled,
        Instant updatedAt
) implements Serializable {

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String providerName = "default";
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String modelName = "gpt-4o-mini";
        private Double temperature = 0.1;
        private Integer timeoutSeconds = 30;
        private boolean isDefault = true;
        private boolean enabled = true;
        private Instant updatedAt = Instant.now();

        public Builder(String id) {
            this.id = id;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AiModelConfig build() {
            return new AiModelConfig(
                    id,
                    providerName,
                    baseUrl,
                    apiKey,
                    modelName,
                    temperature,
                    timeoutSeconds,
                    isDefault,
                    enabled,
                    updatedAt != null ? updatedAt : Instant.now()
            );
        }
    }
}
