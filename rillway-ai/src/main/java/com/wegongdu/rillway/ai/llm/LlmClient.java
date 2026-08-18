package com.wegongdu.rillway.ai.llm;

import java.util.List;
import java.util.Map;

/**
 * Universal LLM Client SPI supporting prompt completion and tool calling.
 */
public interface LlmClient {

    record ToolDefinition(
            String name,
            String description,
            Map<String, Object> parametersSchema
    ) {}

    record ToolCall(
            String callId,
            String toolName,
            Map<String, Object> arguments
    ) {}

    record ToolResult(
            String callId,
            String toolName,
            Object result
    ) {}

    record LlmResponse(
            String content,
            List<ToolCall> toolCalls
    ) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    LlmResponse chat(String systemPrompt, String userPrompt, List<ToolDefinition> availableTools);

    LlmResponse continueChat(String systemPrompt, String userPrompt, List<ToolResult> toolResults);
}
