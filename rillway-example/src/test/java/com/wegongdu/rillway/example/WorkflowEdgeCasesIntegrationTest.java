package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RillwayExampleApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_edge_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class WorkflowEdgeCasesIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ProcessDefinition multiStageDefinition;

    @BeforeEach
    void setup() {
        // 构造一个两级审批流程: start -> dept-leader-approval -> gm-final-approval -> end
        multiStageDefinition = ProcessDefinition.builder("multi-stage-approval-flow")
                .name("多级审批流程")
                .startNode("start", "提交")
                .humanNode("dept-leader-approval", h -> h
                        .name("部门主管初审")
                        .assigneeUser("Manager_Bob")
                )
                .humanNode("gm-final-approval", h -> h
                        .name("总经理终审")
                        .assigneeUser("GM_David")
                )
                .endNode("end", "审批结束")
                .edge("start", "dept-leader-approval")
                .edge("dept-leader-approval", "gm-final-approval")
                .edge("gm-final-approval", "end")
                .build();
    }

    @Test
    @DisplayName("Edge Case 1: Detect final terminal approval node vs intermediate node correctly")
    void should_detect_terminal_approval_node() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("amount", new BigDecimal("8000"))
                .build();

        ProcessInstance instance = processEngine.start(multiStageDefinition, "edge_biz_001", context);
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);

        // 1. 初审阶段：dept-leader-approval
        List<Task> bobTasks = taskService.findPendingTasks("Manager_Bob", List.of());
        assertThat(bobTasks).isNotEmpty();
        Task intermediateTask = bobTasks.get(0);

        // 🎯 验证：中间节点不是终审
        assertThat(taskService.isTerminalTask(intermediateTask.id())).isFalse();
        assertThat(multiStageDefinition.isTerminalNode(intermediateTask.nodeId())).isFalse();

        // 部门主管通过
        ProcessInstance step2Instance = taskService.completeTask(
                intermediateTask.id(),
                ApproveDecision.of(Actor.HumanActor.of("Manager_Bob"), "同意初审")
        );
        assertThat(step2Instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);

        // 2. 终审阶段：gm-final-approval
        List<Task> davidTasks = taskService.findPendingTasks("GM_David", List.of());
        assertThat(davidTasks).isNotEmpty();
        Task finalTask = davidTasks.get(0);

        // 🎯 验证：总经理节点被精准探测为终审节点！
        assertThat(taskService.isTerminalTask(finalTask.id())).isTrue();
        assertThat(multiStageDefinition.isTerminalNode(finalTask.nodeId())).isTrue();

        // 总经理审批通过
        ProcessInstance completedInstance = taskService.completeTask(
                finalTask.id(),
                ApproveDecision.of(Actor.HumanActor.of("GM_David"), "同意采购，批准生效")
        );

        // 验证流程顺利完成
        assertThat(completedInstance.status()).isEqualTo(ProcessStatus.COMPLETED);
    }

    @Test
    @DisplayName("Edge Case 2: Handle employee offboarding/transfer via taskService.transferTask")
    void should_transfer_task_when_employee_offboards() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .build();

        ProcessInstance instance = processEngine.start(multiStageDefinition, "edge_biz_002", context);

        // 初始派给 Manager_Bob
        List<Task> bobTasks = taskService.findPendingTasks("Manager_Bob", List.of());
        assertThat(bobTasks).isNotEmpty();
        String taskId = bobTasks.get(0).id();

        // 🚨 突发状况：Manager_Bob 离职/调岗！管理员介入转办给接任者 New_Manager_Charlie
        Task transferred = taskService.transferTask(taskId, "New_Manager_Charlie", "Bob离职，转交新主管Charlie");
        assertThat(transferred.assigneeUser()).isEqualTo("New_Manager_Charlie");

        // 验证：原人员已查不到该待办
        List<Task> bobTasksAfter = taskService.findPendingTasks("Manager_Bob", List.of());
        assertThat(bobTasksAfter).isEmpty();

        // 验证：新主管成功接收待办并正常审批
        List<Task> charlieTasks = taskService.findPendingTasks("New_Manager_Charlie", List.of());
        assertThat(charlieTasks).isNotEmpty();

        ProcessInstance nextStep = taskService.completeTask(
                charlieTasks.get(0).id(),
                ApproveDecision.of(Actor.HumanActor.of("New_Manager_Charlie"), "新主管审批同意")
        );
        assertThat(nextStep.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
    }

    @Test
    @DisplayName("Edge Case 3: Auto-escalation and fallback when leader is missing/inactive")
    void should_escalate_and_fallback_when_leader_missing() {
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            // 设置员工 Eva，所在部门 DEPT_HR，但直属领导为空（离职）
            defaultIdentity.registerUserProfile(UserProfile.builder("Eva")
                    .departmentId("DEPT_HR")
                    .directLeaderId(null) // 领导离职空缺
                    .build());

            // 设置 DEPT_HR 部门负责人为 HR_Director_Helen
            defaultIdentity.registerDepartmentManager("DEPT_HR", "HR_Director_Helen");
        }

        // 自然语言动态指派：让申请人的直属领导审批
        ProcessDefinition dynamicLeaderDefinition = ProcessDefinition.builder("dynamic-escalation-flow")
                .name("领导空缺顺延流程")
                .startNode("start", "提交")
                .humanNode("leader-step", h -> h
                        .name("领导审批")
                        .assigneePrompt("让申请人的直属领导审批")
                )
                .endNode("end", "结束")
                .edge("start", "leader-step")
                .edge("leader-step", "end")
                .build();

        ProcessContext context = ProcessContext.builder()
                .initiator("Eva")
                .build();

        // 🚀 核心验证：直属领导空缺时，引擎自动顺延自愈到部门总监 HR_Director_Helen，绝不抛出异常崩溃！
        ProcessInstance instance = processEngine.start(dynamicLeaderDefinition, "edge_biz_003", context);
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);

        List<Task> helenTasks = taskService.findPendingTasks("HR_Director_Helen", List.of());
        assertThat(helenTasks).isNotEmpty();

        taskService.completeTask(helenTasks.get(0).id(), ApproveDecision.of(Actor.HumanActor.of("HR_Director_Helen"), "部门总监代批通过"));
    }
}
