package com.wegongdu.rillway.example.service;

import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.ai.intent.ProcessIntent;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.example.model.PromptCompileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流自然语言编译服务
 * 封装 LLM 意图编译器，支持前端输入自然语言制度实时生成标准 DAG 流程图
 */
@Service
public class WorkflowCompilationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCompilationService.class);

    @Autowired
    private IntentInterpreter intentInterpreter;

    /**
     * 将自然语言审批制度文本编译为可执行的 DAG ProcessDefinition
     */
    public Map<String, Object> compilePromptToDag(PromptCompileRequest request) {
        log.info("🧠 [流程编译器] 收到前端 Prompt 编译请求:\n{}", request.getPrompt());

        ProcessContext context = (request.getVariables() != null)
                ? ProcessContext.of(request.getVariables())
                : ProcessContext.empty();

        ProcessDefinition definition = intentInterpreter.interpret(ProcessIntent.of(
                request.getPrompt(),
                request.getInitiator() != null ? request.getInitiator() : "user_001",
                context
        ));

        // 组装返回给前端可视化呈现的 DAG 结构
        Map<String, Object> result = new HashMap<>();
        result.put("definitionId", definition.id());
        result.put("definitionName", definition.name());
        result.put("version", definition.version());
        result.put("nodes", definition.nodes().values().stream().map(node -> Map.of(
                "id", node.id(),
                "name", node.name(),
                "type", node.type()
        )).toList());
        result.put("edges", definition.edges().stream().map(edge -> Map.of(
                "source", edge.sourceNodeId(),
                "target", edge.targetNodeId(),
                "conditionDescription", edge.conditionDescription() != null ? edge.conditionDescription() : ""
        )).toList());

        log.info("✨ [流程编译器] 成功编译生成 DAG 流程: ID={}, 节点数={}, 连线数={}",
                definition.id(), definition.nodes().size(), definition.edges().size());

        return result;
    }
}
