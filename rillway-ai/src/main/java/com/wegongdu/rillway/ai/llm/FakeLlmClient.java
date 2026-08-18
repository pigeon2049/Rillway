package com.wegongdu.rillway.ai.llm;

import java.util.List;
import java.util.Map;

/**
 * Default simulated LLM client for zero-config startup and unit tests.
 */
public class FakeLlmClient implements LlmClient {

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt, List<ToolDefinition> availableTools) {
        if (availableTools != null && availableTools.stream().anyMatch(t -> "buildWorkflowDag".equals(t.name()))) {
            Map<String, Object> args = Map.of(
                    "processId", "auto_compiled_flow",
                    "processName", "Auto Compiled Workflow",
                    "nodes", List.of(
                            Map.of("id", "start", "type", "START", "name", "Start"),
                            Map.of("id", "leader_approval", "type", "HUMAN", "name", "Leader Approval", "assigneePrompt", "Direct Leader"),
                            Map.of("id", "end", "type", "END", "name", "End")
                    ),
                    "edges", List.of(
                            Map.of("source", "start", "target", "leader_approval"),
                            Map.of("source", "leader_approval", "target", "end")
                    )
            );
            return new LlmResponse(null, List.of(new ToolCall("call_mock_1", "buildWorkflowDag", args)));
        }
        return new LlmResponse("OK", List.of());
    }

    @Override
    public LlmResponse continueChat(String systemPrompt, String userPrompt, List<ToolResult> toolResults) {
        return new LlmResponse("COMPLETED", List.of());
    }
}
