package com.wegongdu.rillway.ai.trace;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit trace record for LLM API calls and Tool method invocations.
 */
public record LlmTraceRecord(
        String id,
        String traceId,
        String nodeId,
        String businessType,
        String model,
        String callType,
        String promptText,
        String responseText,
        String toolName,
        String toolArguments,
        String toolResult,
        String toolCallsJson,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long latencyMs,
        String status,
        String errorMessage,
        Instant createdAt
) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private String traceId;
        private String nodeId;
        private String businessType;
        private String model;
        private String callType = "CHAT";
        private String promptText;
        private String responseText;
        private String toolName;
        private String toolArguments;
        private String toolResult;
        private String toolCallsJson;
        private Integer promptTokens = 0;
        private Integer completionTokens = 0;
        private Integer totalTokens = 0;
        private long latencyMs = 0;
        private String status = "SUCCESS";
        private String errorMessage;
        private Instant createdAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder businessType(String businessType) { this.businessType = businessType; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder callType(String callType) { this.callType = callType; return this; }
        public Builder promptText(String promptText) { this.promptText = promptText; return this; }
        public Builder responseText(String responseText) { this.responseText = responseText; return this; }
        public Builder toolName(String toolName) { this.toolName = toolName; return this; }
        public Builder toolArguments(String toolArguments) { this.toolArguments = toolArguments; return this; }
        public Builder toolResult(String toolResult) { this.toolResult = toolResult; return this; }
        public Builder toolCallsJson(String toolCallsJson) { this.toolCallsJson = toolCallsJson; return this; }
        public Builder promptTokens(Integer promptTokens) { this.promptTokens = promptTokens; return this; }
        public Builder completionTokens(Integer completionTokens) { this.completionTokens = completionTokens; return this; }
        public Builder totalTokens(Integer totalTokens) { this.totalTokens = totalTokens; return this; }
        public Builder latencyMs(long latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public LlmTraceRecord build() {
            return new LlmTraceRecord(
                    id, traceId, nodeId, businessType, model, callType,
                    promptText, responseText, toolName, toolArguments, toolResult,
                    toolCallsJson, promptTokens, completionTokens, totalTokens,
                    latencyMs, status, errorMessage, createdAt
            );
        }
    }
}
