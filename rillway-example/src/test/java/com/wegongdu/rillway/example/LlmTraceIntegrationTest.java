package com.wegongdu.rillway.example;

import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.ai.llm.OpenAiCompatibleLlmClient;
import com.wegongdu.rillway.ai.trace.LlmTraceRecord;
import com.wegongdu.rillway.ai.trace.LlmTraceSink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RillwayExampleApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_trace_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "rillway.ai.trace.enabled=true",
        "rillway.ai.trace.log-payload=true"
})
class LlmTraceIntegrationTest {

    @Autowired
    private LlmTraceSink llmTraceSink;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should auto-create rillway_ai_trace table and record trace logs")
    void should_record_and_query_llm_traces() {
        // 1. 验证 rillway_ai_trace 自动建表成功
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rillway_ai_trace", Integer.class);
        assertThat(count).isNotNull();

        // 2. 插入一条标准的大模型与方法调用追溯日志
        String traceId = "trace_proc_inst_9988";
        LlmTraceRecord record = LlmTraceRecord.builder()
                .traceId(traceId)
                .nodeId("dept-manager-approval")
                .businessType("purchase_order")
                .model("deepseek-chat")
                .callType("CHAT")
                .promptText("请查询申请人 Alice 的直属领导")
                .responseText("申请人 Alice 的直属领导为 Manager_Bob")
                .toolName("getDirectLeader")
                .toolArguments("{\"userId\":\"Alice\"}")
                .toolResult("\"Manager_Bob\"")
                .promptTokens(128)
                .completionTokens(45)
                .totalTokens(173)
                .latencyMs(320)
                .status("SUCCESS")
                .build();

        llmTraceSink.record(record);

        // 3. 通过 Trace ID 查询追溯链路
        List<LlmTraceRecord> traces = llmTraceSink.findByTraceId(traceId);
        assertThat(traces).hasSize(1);

        LlmTraceRecord saved = traces.get(0);
        assertThat(saved.traceId()).isEqualTo(traceId);
        assertThat(saved.nodeId()).isEqualTo("dept-manager-approval");
        assertThat(saved.businessType()).isEqualTo("purchase_order");
        assertThat(saved.model()).isEqualTo("deepseek-chat");
        assertThat(saved.toolName()).isEqualTo("getDirectLeader");
        assertThat(saved.toolArguments()).contains("Alice");
        assertThat(saved.toolResult()).contains("Manager_Bob");
        assertThat(saved.totalTokens()).isEqualTo(173);
        assertThat(saved.latencyMs()).isEqualTo(320L);
        assertThat(saved.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("should parse OpenAI response with tokens and record trace automatically")
    void should_parse_openai_response_and_track_meta() throws Exception {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                "http://127.0.0.1:9999/v1", "sk-test", "deepseek-chat", 0.1, Duration.ofSeconds(5), llmTraceSink
        );

        String mockOpenAiJson = """
        {
            "id": "chatcmpl-12345",
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "已同意采购",
                        "tool_calls": [
                            {
                                "id": "call_abc123",
                                "type": "function",
                                "function": {
                                    "name": "getDepartmentManager",
                                    "arguments": "{\\"departmentId\\":\\"DEPT_RD\\"}"
                                }
                            }
                        ]
                    }
                }
            ],
            "usage": {
                "prompt_tokens": 150,
                "completion_tokens": 30,
                "total_tokens": 180
            }
        }
        """;

        OpenAiCompatibleLlmClient.ChatExecutionResult result = client.parseOpenAiResponseWithMeta(mockOpenAiJson);
        assertThat(result.response().content()).isEqualTo("已同意采购");
        assertThat(result.response().toolCalls()).hasSize(1);
        assertThat(result.response().toolCalls().get(0).toolName()).isEqualTo("getDepartmentManager");
        assertThat(result.meta().totalTokens()).isEqualTo(180);
    }
}

