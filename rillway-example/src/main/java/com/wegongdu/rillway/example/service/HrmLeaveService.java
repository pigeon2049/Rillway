package com.wegongdu.rillway.example.service;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.example.hrm.HrmAttendanceLeave;
import com.wegongdu.rillway.example.model.LeaveApplyRequest;
import com.wegongdu.rillway.example.model.TaskDecisionRequest;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.preview.PreviewContext;
import com.wegongdu.rillway.runtime.preview.ProcessPreview;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HRM 考勤请假单业务服务
 * 演示通过 ProcessEngine 实现零代码/声明式流程发起、预测预览与审批办理
 */
@Service
public class HrmLeaveService {

    private static final Logger log = LoggerFactory.getLogger(HrmLeaveService.class);

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessPreviewer processPreviewer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 1. 发起请假申请流程 (直接传入业务实体 Bean，自动匹配 BindingConfig / Prompt 驱动)
     */
    @Transactional
    public ProcessInstance applyLeave(LeaveApplyRequest request) {
        Long leaveId = System.currentTimeMillis();
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalDateTime.now().plusDays(request.getDay().longValue());

        // 插入业务持久化表 (初始状态 1: 审批中)
        jdbcTemplate.update("""
            INSERT INTO hrm_attendance_leave (
                id, employee_id, type, start_time, end_time, "day", reason, approval_status, tenant_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1)
        """, leaveId, request.getEmployeeId(), request.getType(), startTime, endTime, request.getDay(), request.getReason());

        HrmAttendanceLeave leave = HrmAttendanceLeave.builder()
                .id(leaveId)
                .employeeId(request.getEmployeeId())
                .type(request.getType())
                .startTime(startTime)
                .endTime(endTime)
                .day(request.getDay())
                .reason(request.getReason())
                .approvalStatus(1)
                .tenantId(1L)
                .build();

        log.info("🚀 [业务服务] 发起请假申请: ID={}, 申请人={}, 请假天数={} 天, 类型={}",
                leaveId, leave.getEmployeeId(), leave.getDay(), leave.getType());

        // 核心：直接把业务实体传给 ProcessEngine 启动流程
        return processEngine.start(leave);
    }

    /**
     * 2. 提交前预测预览审批链路 (ProcessPreviewer)
     */
    public ProcessPreview previewLeaveWorkflow(LeaveApplyRequest request) {
        HrmAttendanceLeave leave = HrmAttendanceLeave.builder()
                .employeeId(request.getEmployeeId())
                .type(request.getType())
                .day(request.getDay())
                .reason(request.getReason())
                .build();

        // 示例预置请假标准流程
        ProcessDefinition leaveDef = ProcessDefinition.builder("hrm_leave_preview")
                .name("HRM 考勤审批标准流程")
                .startNode("start", "提交申请")
                .humanNode("dept_leader", b -> b.name("直属主管审批").assigneeUser("10"))
                .humanNode("gm", b -> b.name("总经理终审").assigneeUser("1"))
                .endNode("end", "流程结束")
                .edge("start", "dept_leader")
                .edge("dept_leader", "gm")
                .edge("gm", "end")
                .build();

        return processPreviewer.preview(leaveDef, PreviewContext.of(
                request.getEmployeeId(),
                ProcessContext.from(leave)
        ));
    }

    /**
     * 3. 审批通过待办任务
     */
    public ProcessInstance approveTask(String taskId, TaskDecisionRequest request) {
        log.info("👉 [审批处理] 审批人 [{}] 批准任务 [{}]，意见: '{}'", request.getActorId(), taskId, request.getComment());
        return taskService.completeTask(taskId, ApproveDecision.of(
                Actor.HumanActor.of(request.getActorId()),
                request.getComment() != null ? request.getComment() : "同意"
        ));
    }

    /**
     * 4. 驳回待办任务
     */
    public ProcessInstance rejectTask(String taskId, TaskDecisionRequest request) {
        log.warn("❌ [审批处理] 审批人 [{}] 驳回任务 [{}]，原因: '{}'", request.getActorId(), taskId, request.getComment());
        return taskService.completeTask(taskId, RejectDecision.of(
                Actor.HumanActor.of(request.getActorId()),
                request.getComment() != null ? request.getComment() : "驳回"
        ));
    }

    /**
     * 5. 查询指定用户待办列表
     */
    public List<Task> getPendingTasksForUser(String userId) {
        return taskService.findPendingTasks(userId, Collections.emptyList());
    }
}
