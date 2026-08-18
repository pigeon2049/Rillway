package com.wegongdu.rillway.ai.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleLlmClientTest {

    @Test
    void parseOpenAiResponse_withTextContent_shouldParseCorrectly() throws Exception {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient("https://api.openai.com/v1", "test-key", "gpt-4o-mini");

        String sampleJsonResponse = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-4o-mini",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "Alice's direct leader is Bob."
                },
                "finish_reason": "stop"
              }],
              "usage": {
                "prompt_tokens": 9,
                "completion_tokens": 12,
                "total_tokens": 21
              }
            }
            """;

        LlmClient.LlmResponse response = client.parseOpenAiResponse(sampleJsonResponse);
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Alice's direct leader is Bob.");
        assertThat(response.hasToolCalls()).isFalse();
        assertThat(response.toolCalls()).isEmpty();
    }

    @Test
    void parseOpenAiResponse_withToolCalls_shouldParseCorrectly() throws Exception {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient("https://api.openai.com/v1", "test-key", "gpt-4o-mini");

        String sampleToolCallsResponse = """
            {
              "id": "chatcmpl-456",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-4o-mini",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": null,
                  "tool_calls": [
                    {
                      "id": "call_abc123",
                      "type": "function",
                      "function": {
                        "name": "getDirectLeader",
                        "arguments": "{\\"userId\\": \\"Alice\\"}"
                      }
                    }
                  ]
                },
                "finish_reason": "tool_calls"
              }]
            }
            """;

        LlmClient.LlmResponse response = client.parseOpenAiResponse(sampleToolCallsResponse);
        assertThat(response).isNotNull();
        assertThat(response.hasToolCalls()).isTrue();
        assertThat(response.toolCalls()).hasSize(1);

        LlmClient.ToolCall call = response.toolCalls().get(0);
        assertThat(call.callId()).isEqualTo("call_abc123");
        assertThat(call.toolName()).isEqualTo("getDirectLeader");
        assertThat(call.arguments()).containsEntry("userId", "Alice");
    }
}
