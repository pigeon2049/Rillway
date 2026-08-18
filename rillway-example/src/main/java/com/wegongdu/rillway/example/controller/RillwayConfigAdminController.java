package com.wegongdu.rillway.example.controller;

import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.ai.intent.ProcessIntent;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.example.model.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rillway 7 张核心内置表管理与流程一键生成控制台 API
 */
@Tag(name = "Rillway 7张核心底表管理与流程生成控制台 API", description = "提供单据规则绑定、大模型配置热插拔、决策缓存、AI追溯、实例任务与流转历史全方位管理")
@RestController
@RequestMapping("/api/admin")
public class RillwayConfigAdminController {

    private static final Logger log = LoggerFactory.getLogger(RillwayConfigAdminController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IntentInterpreter intentInterpreter;

    // =========================================================================
    // 1. 表 rillway_binding_config (业务单据绑定与自然语言制度规则)
    // =========================================================================

    @Operation(summary = "1.1 查询业务单据绑定规则列表 (rillway_binding_config)")
    @GetMapping("/binding-config")
    public CommonResult<List<Map<String, Object>>> listBindingConfigs() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM rillway_binding_config ORDER BY id ASC");
        return CommonResult.success(list);
    }

    @Operation(summary = "1.2 保存或更新业务单据绑定配置 (rillway_binding_config)")
    @PostMapping("/binding-config")
    public CommonResult<String> saveOrUpdateBindingConfig(@RequestBody Map<String, Object> body) {
        String id = (String) body.getOrDefault("id", "cfg_" + UUID.randomUUID().toString().substring(0, 8));
        String businessType = (String) body.get("businessType");
        String processDefinitionId = (String) body.getOrDefault("processDefinitionId", businessType);
        String processPrompt = (String) body.get("processPrompt");
        String tableName = (String) body.get("tableName");
        String primaryKeyColumn = (String) body.getOrDefault("primaryKeyColumn", "id");
        String statusColumn = (String) body.get("statusColumn");
        String approvedValue = (String) body.get("approvedValue");
        String rejectedValue = (String) body.get("rejectedValue");
        String runningValue = (String) body.getOrDefault("runningValue", "1");
        Boolean enabled = (Boolean) body.getOrDefault("enabled", true);

        jdbcTemplate.update("""
            MERGE INTO rillway_binding_config (
                id, business_type, process_definition_id, process_prompt, table_name, primary_key_column, status_column, approved_value, rejected_value, running_value, enabled
            ) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, id, businessType, processDefinitionId, processPrompt, tableName, primaryKeyColumn, statusColumn, approvedValue, rejectedValue, runningValue, enabled);

        log.info("💾 [配置管理] 保存业务绑定规则成功: ID={}, 业务类型={}, 对应表={}", id, businessType, tableName);
        return CommonResult.success("业务绑定规则保存成功", id);
    }

    @Operation(summary = "1.3 根据配置表 Prompt 制度一键触发大模型编译并生成 DAG 流程")
    @PostMapping("/binding-config/{id}/generate-flow")
    public CommonResult<Map<String, Object>> generateFlowFromBindingPrompt(@PathVariable String id) {
        Map<String, Object> config = jdbcTemplate.queryForMap("SELECT * FROM rillway_binding_config WHERE id = ?", id);
        String prompt = (String) config.get("process_prompt");
        if (prompt == null || prompt.isBlank()) {
            return CommonResult.error(400, "该配置项未包含 process_prompt 制度描述");
        }

        log.info("🧠 [流程一键生成] 触发大模型编译配置项 [{}] 的自然语言制度:\n{}", id, prompt);
        ProcessDefinition definition = intentInterpreter.interpret(ProcessIntent.of(prompt));

        // 将生成的流程定义 ID 反写更新至 rillway_binding_config
        jdbcTemplate.update("UPDATE rillway_binding_config SET process_definition_id = ? WHERE id = ?", definition.id(), id);

        Map<String, Object> result = Map.of(
                "bindingConfigId", id,
                "generatedDefinitionId", definition.id(),
                "definitionName", definition.name(),
                "nodeCount", definition.nodes().size(),
                "edgeCount", definition.edges().size(),
                "nodes", definition.nodes().values().stream().map(n -> Map.of("id", n.id(), "name", n.name(), "type", n.type())).toList()
        );
        return CommonResult.success("大模型已成功从配置制度自动生成流程 DAG 并绑定", result);
    }

    @Operation(summary = "1.4 删除业务单据绑定配置 (rillway_binding_config)")
    @DeleteMapping("/binding-config/{id}")
    public CommonResult<String> deleteBindingConfig(@PathVariable String id) {
        jdbcTemplate.update("DELETE FROM rillway_binding_config WHERE id = ?", id);
        return CommonResult.success("删除成功", id);
    }

    // =========================================================================
    // 2. 表 rillway_ai_config (大模型连接与 API Key 配置热插拔)
    // =========================================================================

    @Operation(summary = "2.1 查询大模型 API 连接配置列表 (rillway_ai_config)")
    @GetMapping("/ai-config")
    public CommonResult<List<Map<String, Object>>> listAiConfigs() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM rillway_ai_config ORDER BY updated_at DESC");
        return CommonResult.success(list);
    }

    @Operation(summary = "2.2 新增或更新大模型配置 (rillway_ai_config)")
    @PostMapping("/ai-config")
    public CommonResult<String> saveAiConfig(@RequestBody Map<String, Object> body) {
        String id = (String) body.getOrDefault("id", "ai_cfg_" + UUID.randomUUID().toString().substring(0, 8));
        String providerName = (String) body.getOrDefault("providerName", "DeepSeek");
        String baseUrl = (String) body.getOrDefault("baseUrl", "https://api.deepseek.com/v1");
        String apiKey = (String) body.get("apiKey");
        String modelName = (String) body.getOrDefault("modelName", "deepseek-chat");
        Double temperature = Double.valueOf(body.getOrDefault("temperature", 0.1).toString());
        Integer timeoutSeconds = Integer.valueOf(body.getOrDefault("timeoutSeconds", 30).toString());
        Boolean isDefault = (Boolean) body.getOrDefault("isDefault", true);
        Boolean enabled = (Boolean) body.getOrDefault("enabled", true);

        if (isDefault) {
            jdbcTemplate.update("UPDATE rillway_ai_config SET is_default = FALSE");
        }

        jdbcTemplate.update("""
            MERGE INTO rillway_ai_config (
                id, provider_name, base_url, api_key, model_name, temperature, timeout_seconds, is_default, enabled, updated_at
            ) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """, id, providerName, baseUrl, apiKey, modelName, temperature, timeoutSeconds, isDefault, enabled);

        log.info("🤖 [大模型配置热插拔] 更新大模型配置: ID={}, Provider={}, Model={}", id, providerName, modelName);
        return CommonResult.success("大模型配置保存成功", id);
    }

    // =========================================================================
    // 3. 表 rillway_resolution_cache (大模型决策分支隔离快照缓存)
    // =========================================================================

    @Operation(summary = "3.1 查询大模型决策分支缓存快照 (rillway_resolution_cache)")
    @GetMapping("/resolution-cache")
    public CommonResult<List<Map<String, Object>>> listResolutionCaches() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM rillway_resolution_cache ORDER BY created_at DESC LIMIT 50");
        return CommonResult.success(list);
    }

    @Operation(summary = "3.2 一键清空或按流程定义清解决策缓存 (rillway_resolution_cache)")
    @DeleteMapping("/resolution-cache")
    public CommonResult<String> clearResolutionCache(@RequestParam(required = false) String definitionId) {
        if (definitionId != null && !definitionId.isBlank()) {
            int rows = jdbcTemplate.update("DELETE FROM rillway_resolution_cache WHERE definition_id = ?", definitionId);
            return CommonResult.success("已清除流程 [" + definitionId + "] 的决策缓存: " + rows + " 条", definitionId);
        } else {
            int rows = jdbcTemplate.update("DELETE FROM rillway_resolution_cache");
            return CommonResult.success("已清空全部大模型决策快照缓存，共清除 " + rows + " 条", "ALL");
        }
    }

    // =========================================================================
    // 4. 表 rillway_ai_trace (大模型与 Tool Calling 审计日志)
    // =========================================================================

    @Operation(summary = "4.1 查询大模型调用全链路追溯与审计日志 (rillway_ai_trace)")
    @GetMapping("/ai-trace")
    public CommonResult<List<Map<String, Object>>> listAiTraces(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String businessType) {
        String sql = (businessType != null && !businessType.isBlank())
                ? "SELECT * FROM rillway_ai_trace WHERE business_type = ? ORDER BY id DESC LIMIT ?"
                : "SELECT * FROM rillway_ai_trace ORDER BY id DESC LIMIT ?";

        List<Map<String, Object>> list = (businessType != null && !businessType.isBlank())
                ? jdbcTemplate.queryForList(sql, businessType, limit)
                : jdbcTemplate.queryForList(sql, limit);
        return CommonResult.success(list);
    }

    @Operation(summary = "4.2 清理历史 AI 调用追溯日志 (rillway_ai_trace)")
    @DeleteMapping("/ai-trace")
    public CommonResult<String> clearAiTraces() {
        int rows = jdbcTemplate.update("DELETE FROM rillway_ai_trace");
        return CommonResult.success("已清理历史 AI 审计日志: " + rows + " 条", String.valueOf(rows));
    }

    // =========================================================================
    // 5. 表 rillway_instance (流程实例运行时监控)
    // =========================================================================

    @Operation(summary = "5.1 查询全量流程实例列表 (rillway_instance)")
    @GetMapping("/instances")
    public CommonResult<List<Map<String, Object>>> listInstances(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        String sql = (status != null && !status.isBlank())
                ? "SELECT * FROM rillway_instance WHERE status = ? ORDER BY started_at DESC LIMIT ?"
                : "SELECT * FROM rillway_instance ORDER BY started_at DESC LIMIT ?";

        List<Map<String, Object>> list = (status != null && !status.isBlank())
                ? jdbcTemplate.queryForList(sql, status, limit)
                : jdbcTemplate.queryForList(sql, limit);
        return CommonResult.success(list);
    }

    @Operation(summary = "5.2 管理员强制终止/作废流程实例 (rillway_instance)")
    @PostMapping("/instances/{instanceId}/terminate")
    public CommonResult<String> terminateInstance(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "管理员后台手动终止") String reason) {
        jdbcTemplate.update("""
            UPDATE rillway_instance
            SET status = 'TERMINATED', error_message = ?, completed_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """, reason, instanceId);

        // 同步取消关联的未完成待办任务
        jdbcTemplate.update("UPDATE rillway_task SET status = 'CANCELLED', completed_at = CURRENT_TIMESTAMP WHERE process_instance_id = ? AND status = 'PENDING'", instanceId);

        log.warn("⚠️ [控制台管理] 流程实例 [{}] 已被强制终止，原因: {}", instanceId, reason);
        return CommonResult.success("流程实例已终止", instanceId);
    }

    // =========================================================================
    // 6. 表 rillway_task (全员待办任务管理)
    // =========================================================================

    @Operation(summary = "6.1 查询所有任务列表 (rillway_task)")
    @GetMapping("/tasks")
    public CommonResult<List<Map<String, Object>>> listAllTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        String sql = (status != null && !status.isBlank())
                ? "SELECT * FROM rillway_task WHERE status = ? ORDER BY created_at DESC LIMIT ?"
                : "SELECT * FROM rillway_task ORDER BY created_at DESC LIMIT ?";

        List<Map<String, Object>> list = (status != null && !status.isBlank())
                ? jdbcTemplate.queryForList(sql, status, limit)
                : jdbcTemplate.queryForList(sql, limit);
        return CommonResult.success(list);
    }

    @Operation(summary = "6.2 管理员强制重派任务审批人 (rillway_task)")
    @PostMapping("/tasks/{taskId}/reassign")
    public CommonResult<String> reassignTask(
            @PathVariable String taskId,
            @RequestParam String targetUserId,
            @RequestParam(defaultValue = "管理员后台强制委派") String reason) {
        jdbcTemplate.update("UPDATE rillway_task SET assignee_user = ? WHERE id = ?", targetUserId, taskId);
        log.info("👉 [任务重派] 任务 [{}] 审批人已被管理员重新指派为 [{}], 备注: {}", taskId, targetUserId, reason);
        return CommonResult.success("任务已重派给用户: " + targetUserId, taskId);
    }

    // =========================================================================
    // 7. 表 rillway_history (流程节点执行审计历史)
    // =========================================================================

    @Operation(summary = "7.1 查询指定流程实例的完整流转历史流水 (rillway_history)")
    @GetMapping("/history/{instanceId}")
    public CommonResult<List<Map<String, Object>>> getInstanceHistory(@PathVariable String instanceId) {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT * FROM rillway_history WHERE process_instance_id = ? ORDER BY entered_at ASC", instanceId);
        return CommonResult.success(history);
    }
}
