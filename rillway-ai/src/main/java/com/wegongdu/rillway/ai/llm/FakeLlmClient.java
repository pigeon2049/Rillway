package com.wegongdu.rillway.ai.llm;

import java.util.List;

/**
 * Default simulated LLM client for zero-config startup and unit tests.
 */
public class FakeLlmClient implements LlmClient {

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt, List<ToolDefinition> availableTools) {
        return new LlmResponse("OK", List.of());
    }

    @Override
    public LlmResponse continueChat(String systemPrompt, String userPrompt, List<ToolResult> toolResults) {
        return new LlmResponse("COMPLETED", List.of());
    }
}
