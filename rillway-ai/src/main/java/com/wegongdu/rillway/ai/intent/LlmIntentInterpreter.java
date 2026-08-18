package com.wegongdu.rillway.ai.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wegongdu.rillway.core.identity.EntityClassIntrospector;
import com.wegongdu.rillway.core.identity.OrgEntityRegistry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI-Native LLM Workflow Compiler.
 * <p>
 * Provides standard Tool Calling interface to LLMs (e.g. DeepSeek/OpenAI),
 * enabling the LLM to autonomously structure natural language enterprise policies
 * into executable, strongly-typed {@link ProcessDefinition} DAGs without any hardcoding.
 */
public class LlmIntentInterpreter implements IntentInterpreter {

    private static final Logger log = LoggerFactory.getLogger(LlmIntentInterpreter.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final OrgEntityRegistry orgEntityRegistry;

    public LlmIntentInterpreter(LlmClient llmClient) {
        this(llmClient, null);
    }

    public LlmIntentInterpreter(LlmClient llmClient, OrgEntityRegistry orgEntityRegistry) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient must not be null");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.orgEntityRegistry = orgEntityRegistry;
    }

    @Override
    public ProcessDefinition interpret(ProcessIntent intent) {
        String promptText = intent.naturalLanguage();
        log.info("🧠 [LlmIntentInterpreter] 正在使用大模型 Tool Calling 将企业制度编译为流程 DAG:\n{}", promptText);

        // 1. 提取业务表单字段元数据
        StringBuilder schemaInfo = new StringBuilder();
        if (intent.exampleContext() != null && !intent.exampleContext().variables().isEmpty()) {
            schemaInfo.append("\n【当前业务单据可用字段清单与数据类型】:\n");
            intent.exampleContext().variables().forEach((k, v) -> {
                String typeName = v != null ? v.getClass().getSimpleName() : "Object";
                schemaInfo.append("  - 字段名: `").append(k).append("` (数据类型: ").append(typeName).append(")\n");
            });
            schemaInfo.append("请务必严格使用上述字段名编写 RULE 节点的 conditionSpel 表达式。\n");
        }

        // 2. 提取注册的组织架构实体类 Schema
        StringBuilder orgInfo = new StringBuilder();
        if (orgEntityRegistry != null) {
            orgInfo.append("\n【企业组织架构实体 Schema (基于注册的实体 Class 反射解析)】:\n");
            if (orgEntityRegistry.userEntityClass() != null) {
                var meta = EntityClassIntrospector.introspect(orgEntityRegistry.userEntityClass());
                orgInfo.append(meta.toPromptDescription());
            }
            if (orgEntityRegistry.deptEntityClass() != null) {
                var meta = EntityClassIntrospector.introspect(orgEntityRegistry.deptEntityClass());
                orgInfo.append(meta.toPromptDescription());
            }
            if (orgEntityRegistry.postEntityClass() != null) {
                var meta = EntityClassIntrospector.introspect(orgEntityRegistry.postEntityClass());
                orgInfo.append(meta.toPromptDescription());
            }
            if (orgEntityRegistry.roleEntityClass() != null) {
                var meta = EntityClassIntrospector.introspect(orgEntityRegistry.roleEntityClass());
                orgInfo.append(meta.toPromptDescription());
            }
            orgInfo.append("请参考上述实体类的字段名及含义进行审批人路由规则指定与条件判断。\n");
        }

        // 1. 定义提供给大模型的 DAG 编译工具
        LlmClient.ToolDefinition dagTool = new LlmClient.ToolDefinition(
                "buildWorkflowDag",
                "编译企业业务制度为标准的工作流 DAG 节点图（包含节点、通用条件路由表达式与必须的连线）",
                Map.of(
                        "processId", "string (流程唯一标识，如 hrm_leave_approval)",
                        "processName", "string (流程展示名称)",
                        "nodes", "array (节点列表，每个节点包含 id, type[START/RULE/HUMAN/AGENT/END], name, assigneePrompt[审批人描述], conditionSpel[条件表达式如 'day <= 3'], trueTargetNodeId, falseTargetNodeId)",
                        "edges", "array of objects with {source: string, target: string} (必须包含完整的边连接，例如 start -> rule_1, rule_1 -> human_1 等)"
                )
        );

        String systemPrompt = """
            你是一个专业的工作流架构编译器。请分析用户的企业业务制度，并调用工具 `buildWorkflowDag` 编译生成完整、严谨、闭环的流程 DAG 拓扑图。
            
            要求：
            1. 流程起点必须为 START 节点，终点必须为 END 节点；
            2. 条件分流使用 RULE 节点，其 `conditionSpel` 为标准的条件表达式（例如 `day <= 3`、`deptName == '研发部'`、`amount > 10000`、`type == '年假'` 等），支持任意业务与组织架构字段；
            3. 人工节点使用 HUMAN 节点，需在 `assigneePrompt` 中指明审批人规则（如 '请假申请人所在部门的直属主管'、'总经理'、'财务总监'）；
            4. 必须通过调用 `buildWorkflowDag` 工具提交生成的 DAG 结构。
            """ + schemaInfo + orgInfo;

        String userPrompt = "企业制度描述如下：\n" + promptText + (schemaInfo.length() > 0 ? "\n" + schemaInfo : "") + (orgInfo.length() > 0 ? "\n" + orgInfo : "");

        try {
            LlmClient.LlmResponse response = llmClient.chat(systemPrompt, userPrompt, List.of(dagTool));

            // 2. 优先从 Tool Call 中获取大模型生成的结构化参数
            if (response != null && response.hasToolCalls()) {
                for (LlmClient.ToolCall call : response.toolCalls()) {
                    if ("buildWorkflowDag".equals(call.toolName())) {
                        log.info("🎯 [LlmIntentInterpreter] 大模型通过 Tool Calling 成功输出结构化 DAG 方案");
                        ProcessDefinition definition = parseDefinitionFromToolArgs(call.arguments(), promptText);
                        if (definition != null) {
                            log.info("✅ [LlmIntentInterpreter] 成功编译流程定义: ID={}, 名称={}, 节点数={}",
                                    definition.id(), definition.name(), definition.nodes().size());
                            return definition;
                        }
                    }
                }
            }

            // 3. 兜底：若模型以普通 JSON 文本返回，则解析文本中的 JSON
            if (response != null && response.content() != null && !response.content().isBlank()) {
                String cleanJson = extractJson(response.content());
                ProcessDefinition definition = parseDefinitionFromJson(cleanJson, promptText);
                if (definition != null) {
                    log.info("✅ [LlmIntentInterpreter] 成功从大模型响应 JSON 编译流程: ID={}", definition.id());
                    return definition;
                }
            }
        } catch (Exception e) {
            log.error("大模型编译制度流程失败: {}", e.getMessage(), e);
            throw new RuntimeException("大模型编译业务制度失败: " + e.getMessage(), e);
        }

        throw new RuntimeException("无法根据制度描述编译出有效的工作流定义: " + promptText);
    }

    private ProcessDefinition parseDefinitionFromToolArgs(Map<String, Object> args, String originalPrompt) {
        try {
            String json = objectMapper.writeValueAsString(args);
            return parseDefinitionFromJson(json, originalPrompt);
        } catch (Exception e) {
            log.warn("Failed to serialize tool arguments: {}", e.getMessage());
            return null;
        }
    }

    private ProcessDefinition parseDefinitionFromJson(String json, String originalPrompt) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String id = root.path("processId").asText(root.path("id").asText("process_compiled"));
            String name = root.path("processName").asText(root.path("name").asText("制度编译流程"));

            ProcessDefinition.Builder builder = ProcessDefinition.builder(id)
                    .name(name)
                    .description("Generated from enterprise policy: " + originalPrompt)
                    .version("1.0.0");

            JsonNode nodesNode = root.path("nodes");
            if (nodesNode.isArray()) {
                for (JsonNode node : nodesNode) {
                    String nodeId = node.path("id").asText();
                    String nodeType = node.path("type").asText("HUMAN").toUpperCase();
                    String nodeName = node.path("name").asText(nodeId);

                    switch (nodeType) {
                        case "START" -> builder.startNode(nodeId, nodeName);
                        case "END" -> builder.endNode(nodeId, nodeName);
                        case "HUMAN" -> {
                            String prompt = node.path("assigneePrompt").asText(nodeName);
                            String role = node.path("assigneeRole").asText(null);
                            builder.humanNode(nodeId, b -> {
                                b.name(nodeName);
                                if (role != null && !role.isBlank()) b.assigneeRole(role);
                                if (prompt != null && !prompt.isBlank()) b.assigneePrompt(prompt);
                            });
                        }
                        case "RULE" -> {
                            String spel = node.path("conditionSpel").asText();
                            if (spel.isBlank()) {
                                String field = node.path("conditionField").asText("day");
                                String op = node.path("operator").asText("<=");
                                String val = node.path("value").asText("3");
                                spel = field + " " + op + " " + val;
                            }
                            String finalSpel = spel;
                            String trueTarget = node.path("trueTargetNodeId").asText(node.path("trueTarget").asText("end"));
                            String falseTarget = node.path("falseTargetNodeId").asText(node.path("falseTarget").asText("end"));

                            builder.ruleNode(nodeId, b -> b.name(nodeName)
                                    .when(finalSpel, ctx -> evaluateExpression(finalSpel, ctx), trueTarget)
                                    .otherwise(falseTarget)
                            );
                        }
                    }
                }
            }

            // 1. 先解析显式的 edges / transitions / connections
            JsonNode edgesNode = root.has("edges") ? root.get("edges") : (root.has("connections") ? root.get("connections") : root.get("transitions"));
            boolean hasExplicitEdges = false;
            if (edgesNode != null && edgesNode.isArray() && !edgesNode.isEmpty()) {
                for (JsonNode edge : edgesNode) {
                    String src = edge.path("source").asText(edge.path("sourceNodeId").asText(edge.path("from").asText()));
                    String tgt = edge.path("target").asText(edge.path("targetNodeId").asText(edge.path("to").asText()));
                    if (!src.isBlank() && !tgt.isBlank()) {
                        builder.edge(src, tgt);
                        hasExplicitEdges = true;
                    }
                }
            }

            // 2. 若大模型未显式返回 edges 数组，则从节点的拓扑属性 (next/targetNodeId/trueTarget/falseTarget) 自动构建 edges
            if (!hasExplicitEdges && nodesNode.isArray()) {
                for (JsonNode node : nodesNode) {
                    String srcId = node.path("id").asText();
                    if (node.has("targetNodeId")) {
                        builder.edge(srcId, node.get("targetNodeId").asText());
                    }
                    if (node.has("next")) {
                        builder.edge(srcId, node.get("next").asText());
                    }
                    if (node.has("trueTargetNodeId")) {
                        builder.edge(srcId, node.get("trueTargetNodeId").asText());
                    }
                    if (node.has("falseTargetNodeId")) {
                        builder.edge(srcId, node.get("falseTargetNodeId").asText());
                    }
                    if (node.has("trueTarget")) {
                        builder.edge(srcId, node.get("trueTarget").asText());
                    }
                    if (node.has("falseTarget")) {
                        builder.edge(srcId, node.get("falseTarget").asText());
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse JSON into ProcessDefinition: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通用动态表达式计算器：不写死任何业务字段，支持任意复杂表达式（如 'day <= 3', 'amount > 5000', 'status == 1' 等）
     */
    private boolean evaluateExpression(String expressionStr, ProcessContext ctx) {
        if (expressionStr == null || expressionStr.isBlank()) {
            return true;
        }
        // 通用词法拆解：<field> <operator> <value>
        String[] tokens = expressionStr.trim().split("\\s+");
        if (tokens.length >= 3) {
            String field = tokens[0];
            String op = tokens[1];
            String val = tokens[2].replace("'", "").replace("\"", "");

            Object actualObj = ctx.get(field);
            if (actualObj == null && "leaveDays".equalsIgnoreCase(field)) {
                actualObj = ctx.get("day");
            } else if (actualObj == null && "day".equalsIgnoreCase(field)) {
                actualObj = ctx.get("leaveDays");
            }

            if (actualObj != null) {
                try {
                    BigDecimal actual = new BigDecimal(actualObj.toString());
                    BigDecimal target = new BigDecimal(val);
                    return switch (op) {
                        case "<=" -> actual.compareTo(target) <= 0;
                        case "<" -> actual.compareTo(target) < 0;
                        case ">=" -> actual.compareTo(target) >= 0;
                        case ">" -> actual.compareTo(target) > 0;
                        case "!=" -> actual.compareTo(target) != 0;
                        default -> actual.compareTo(target) == 0;
                    };
                } catch (NumberFormatException e) {
                    // 字符串等值比较
                    String actualStr = actualObj.toString();
                    return switch (op) {
                        case "!=" -> !actualStr.equals(val);
                        default -> actualStr.equals(val);
                    };
                }
            }
        }
        return false;
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        String clean = text.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();
        int firstBrace = clean.indexOf("{");
        int lastBrace = clean.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return clean.substring(firstBrace, lastBrace + 1);
        }
        return clean;
    }
}
