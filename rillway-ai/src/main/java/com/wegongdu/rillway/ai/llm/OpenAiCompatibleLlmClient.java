package com.wegongdu.rillway.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Lightweight, native HTTP LLM client compatible with the standard OpenAI API specification.
 * Works seamlessly with OpenAI, DeepSeek, Qwen (Aliyun DashScope), Moonshot Kimi, Zhipu GLM, Ollama, etc.
 * Zero external heavyweight SDK dependencies.
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private com.wegongdu.rillway.ai.trace.LlmTraceSink traceSink;

    public OpenAiCompatibleLlmClient(String baseUrl, String apiKey, String model) {
        this(baseUrl, apiKey, model, 0.1, Duration.ofSeconds(30));
    }

    public OpenAiCompatibleLlmClient(String baseUrl, String apiKey, String model, double temperature, Duration timeout) {
        this(baseUrl, apiKey, model, temperature, timeout, null);
    }

    public OpenAiCompatibleLlmClient(String baseUrl, String apiKey, String model, double temperature, Duration timeout, com.wegongdu.rillway.ai.trace.LlmTraceSink traceSink) {
        this.baseUrl = sanitizeBaseUrl(baseUrl);
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.temperature = temperature;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout != null ? timeout : Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.traceSink = traceSink;
    }

    public void setTraceSink(com.wegongdu.rillway.ai.trace.LlmTraceSink traceSink) {
        this.traceSink = traceSink;
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt, List<ToolDefinition> availableTools) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        LlmResponse response = null;
        ParsedTraceMeta traceMeta = null;

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            if (userPrompt != null && !userPrompt.isBlank()) {
                messages.add(Map.of("role", "user", "content", userPrompt));
            }
            requestBody.put("messages", messages);

            if (availableTools != null && !availableTools.isEmpty()) {
                List<Map<String, Object>> tools = buildToolsPayload(availableTools);
                requestBody.put("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            ChatExecutionResult result = executeChatCompletionWithMeta(requestBody);
            response = result.response();
            traceMeta = result.meta();
            return response;
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("Failed to execute OpenAI chat request to [{}], model: [{}]", baseUrl, model, e);
            throw new RuntimeException("OpenAI API request failed: " + e.getMessage(), e);
        } finally {
            recordTrace("CHAT", systemPrompt, userPrompt, response, traceMeta, startTime, status, errorMessage);
        }
    }

    @Override
    public LlmResponse continueChat(String systemPrompt, String userPrompt, List<ToolResult> toolResults) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        LlmResponse response = null;
        ParsedTraceMeta traceMeta = null;

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            if (userPrompt != null && !userPrompt.isBlank()) {
                messages.add(Map.of("role", "user", "content", userPrompt));
            }

            if (toolResults != null && !toolResults.isEmpty()) {
                // 1. Assistant message declaring tool calls
                List<Map<String, Object>> assistantToolCalls = new ArrayList<>();
                for (ToolResult tr : toolResults) {
                    assistantToolCalls.add(Map.of(
                            "id", tr.callId() != null ? tr.callId() : "call_" + UUID.randomUUID(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tr.toolName(),
                                    "arguments", "{}"
                            )
                    ));
                }
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", null);
                assistantMessage.put("tool_calls", assistantToolCalls);
                messages.add(assistantMessage);

                // 2. Tool response messages
                for (ToolResult tr : toolResults) {
                    String contentStr;
                    Object resObj = tr.result();
                    if (resObj instanceof Optional<?> opt) {
                        resObj = opt.orElse(null);
                    }
                    if (resObj instanceof String s) {
                        contentStr = s;
                    } else if (resObj == null) {
                        contentStr = "null";
                    } else {
                        contentStr = objectMapper.writeValueAsString(resObj);
                    }
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", tr.callId() != null ? tr.callId() : "",
                            "name", tr.toolName(),
                            "content", contentStr != null ? contentStr : ""
                    ));
                }
            }
            requestBody.put("messages", messages);

            ChatExecutionResult result = executeChatCompletionWithMeta(requestBody);
            response = result.response();
            traceMeta = result.meta();
            return response;
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("Failed to execute OpenAI continueChat request to [{}], model: [{}]", baseUrl, model, e);
            throw new RuntimeException("OpenAI continueChat request failed: " + e.getMessage(), e);
        } finally {
            recordTrace("CONTINUE_CHAT", systemPrompt, userPrompt, response, traceMeta, startTime, status, errorMessage);
        }
    }

    public record ParsedTraceMeta(Integer promptTokens, Integer completionTokens, Integer totalTokens, String toolCallsJson) {}
    public record ChatExecutionResult(LlmResponse response, ParsedTraceMeta meta) {}

    private ChatExecutionResult executeChatCompletionWithMeta(Map<String, Object> requestBody) throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        if (apiKey != null && !apiKey.isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = reqBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.error("OpenAI API returned error status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("OpenAI API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseOpenAiResponseWithMeta(response.body());
    }

    public LlmResponse parseOpenAiResponse(String responseJson) throws Exception {
        return parseOpenAiResponseWithMeta(responseJson).response();
    }

    public ChatExecutionResult parseOpenAiResponseWithMeta(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("choices");
        if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
            return new ChatExecutionResult(new LlmResponse("", List.of()), new ParsedTraceMeta(0, 0, 0, null));
        }

        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText(null);
        List<ToolCall> toolCalls = new ArrayList<>();
        String toolCallsJson = null;

        if (message.has("tool_calls")) {
            toolCallsJson = message.path("tool_calls").toString();
            for (JsonNode tc : message.path("tool_calls")) {
                String callId = tc.path("id").asText();
                String funcName = tc.path("function").path("name").asText();
                String argsJson = tc.path("function").path("arguments").asText("{}");
                Map<String, Object> arguments = parseArguments(argsJson);
                toolCalls.add(new ToolCall(callId, funcName, arguments));
            }
        }

        JsonNode usage = root.path("usage");
        Integer promptTokens = usage.path("prompt_tokens").asInt(0);
        Integer completionTokens = usage.path("completion_tokens").asInt(0);
        Integer totalTokens = usage.path("total_tokens").asInt(0);

        LlmResponse llmResponse = new LlmResponse(content, toolCalls);
        ParsedTraceMeta meta = new ParsedTraceMeta(promptTokens, completionTokens, totalTokens, toolCallsJson);
        return new ChatExecutionResult(llmResponse, meta);
    }

    private void recordTrace(String callType, String systemPrompt, String userPrompt, LlmResponse response,
                             ParsedTraceMeta traceMeta, long startTime, String status, String errorMessage) {
        if (traceSink == null) return;
        try {
            long latencyMs = System.currentTimeMillis() - startTime;
            String promptText = (systemPrompt != null ? "[System]\n" + systemPrompt + "\n\n" : "") +
                    (userPrompt != null ? "[User]\n" + userPrompt : "");

            com.wegongdu.rillway.ai.trace.LlmTraceRecord record = com.wegongdu.rillway.ai.trace.LlmTraceRecord.builder()
                    .callType(callType)
                    .model(model)
                    .promptText(promptText)
                    .responseText(response != null ? response.content() : null)
                    .toolCallsJson(traceMeta != null ? traceMeta.toolCallsJson() : null)
                    .promptTokens(traceMeta != null ? traceMeta.promptTokens() : 0)
                    .completionTokens(traceMeta != null ? traceMeta.completionTokens() : 0)
                    .totalTokens(traceMeta != null ? traceMeta.totalTokens() : 0)
                    .latencyMs(latencyMs)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            traceSink.record(record);
        } catch (Exception e) {
            log.warn("Failed to record LLM invocation trace", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argsJson) {
        try {
            if (argsJson == null || argsJson.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(argsJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse tool call arguments JSON: {}", argsJson, e);
            return Map.of();
        }
    }

    private List<Map<String, Object>> buildToolsPayload(List<ToolDefinition> availableTools) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolDefinition tool : availableTools) {
            Map<String, Object> functionMap = new LinkedHashMap<>();
            functionMap.put("name", tool.name());
            functionMap.put("description", tool.description());

            Map<String, Object> properties = new LinkedHashMap<>();
            if (tool.parametersSchema() != null) {
                for (Map.Entry<String, Object> entry : tool.parametersSchema().entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> mapVal) {
                        properties.put(entry.getKey(), mapVal);
                    } else if (entry.getValue() instanceof String desc) {
                        String type = "string";
                        if (desc.startsWith("array")) {
                            type = "array";
                        } else if (desc.startsWith("object")) {
                            type = "object";
                        } else if (desc.startsWith("integer") || desc.startsWith("int")) {
                            type = "integer";
                        } else if (desc.startsWith("number")) {
                            type = "number";
                        } else if (desc.startsWith("boolean") || desc.startsWith("bool")) {
                            type = "boolean";
                        }
                        properties.put(entry.getKey(), Map.of("type", type, "description", desc));
                    } else {
                        properties.put(entry.getKey(), Map.of("type", "string"));
                    }
                }
            }

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", properties);
            functionMap.put("parameters", parameters);

            tools.add(Map.of(
                    "type", "function",
                    "function", functionMap
            ));
        }
        return tools;
    }

    private static String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.openai.com/v1";
        }
        String clean = url.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
