package com.wegongdu.rillway.example.controller;

import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.example.model.CommonResult;
import com.wegongdu.rillway.example.model.PromptCompileRequest;
import com.wegongdu.rillway.example.model.TaskDecisionRequest;
import com.wegongdu.rillway.example.service.HrmLeaveService;
import com.wegongdu.rillway.example.service.WorkflowCompilationService;
import com.wegongdu.rillway.runtime.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用工作流控制台 API
 */
@Tag(name = "通用工作流控制台 API", description = "提供自然语言 Prompt 编译、待办审批/驳回/转办、执行轨迹与 AI 审计查询")
@RestController
@RequestMapping("/api/workflow")
public class WorkflowApiController {

    @Autowired
    private WorkflowCompilationService compilationService;

    @Autowired
    private HrmLeaveService hrmLeaveService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Operation(summary = "1. 自然语言 Prompt 即时编译为可执行 DAG 流程图")
    @PostMapping("/compile")
    public CommonResult<Map<String, Object>> compilePrompt(@RequestBody PromptCompileRequest request) {
        return CommonResult.success("编译成功", compilationService.compilePromptToDag(request));
    }

    @Operation(summary = "2. 查询指定用户的待办审批任务列表")
    @GetMapping("/tasks/pending")
    public CommonResult<List<Task>> getPendingTasks(@RequestParam String userId) {
        return CommonResult.success(hrmLeaveService.getPendingTasksForUser(userId));
    }

    @Operation(summary = "3. 办理审批通过")
    @PostMapping("/tasks/{taskId}/approve")
    public CommonResult<com.wegongdu.rillway.example.model.ProcessInstanceDto> approveTask(@PathVariable String taskId, @RequestBody TaskDecisionRequest request) {
        ProcessInstance instance = hrmLeaveService.approveTask(taskId, request);
        return CommonResult.success("审批通过", com.wegongdu.rillway.example.model.ProcessInstanceDto.from(instance));
    }

    @Operation(summary = "4. 办理审批驳回")
    @PostMapping("/tasks/{taskId}/reject")
    public CommonResult<com.wegongdu.rillway.example.model.ProcessInstanceDto> rejectTask(@PathVariable String taskId, @RequestBody TaskDecisionRequest request) {
        ProcessInstance instance = hrmLeaveService.rejectTask(taskId, request);
        return CommonResult.success("已驳回", com.wegongdu.rillway.example.model.ProcessInstanceDto.from(instance));
    }

    @Operation(summary = "5. 任务转办（离职交接或调岗委派）")
    @PostMapping("/tasks/{taskId}/transfer")
    public CommonResult<Task> transferTask(
            @PathVariable String taskId,
            @RequestParam String targetUserId,
            @RequestParam(required = false, defaultValue = "员工岗位调动转办") String reason) {
        Task transferred = taskService.transferTask(taskId, targetUserId, reason);
        return CommonResult.success("转办成功", transferred);
    }

    @Operation(summary = "6. 查询流程实例全量流转历史轨迹")
    @GetMapping("/instances/{instanceId}/history")
    public CommonResult<List<Map<String, Object>>> getInstanceHistory(@PathVariable String instanceId) {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT * FROM rillway_history WHERE process_instance_id = ? ORDER BY entered_at ASC", instanceId);
        return CommonResult.success(history);
    }

    @Operation(summary = "7. 查询大模型 Tool Calling 与决策审计日志")
    @GetMapping("/ai/traces")
    public CommonResult<List<Map<String, Object>>> getAiTraces(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> traces = jdbcTemplate.queryForList(
                "SELECT id, model, prompt_text, response_text, total_tokens, latency_ms, status, created_at FROM rillway_ai_trace ORDER BY id DESC LIMIT ?", limit);
        return CommonResult.success(traces);
    }
}
