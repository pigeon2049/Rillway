package com.wegongdu.rillway.example;

import com.wegongdu.rillway.ai.cache.ResolutionCacheRepository;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseApplication.class)
class ConditionalBranchWorkflowIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private ResolutionCacheRepository resolutionCacheRepository;

    private ProcessDefinition leaveWorkflow;

    @BeforeEach
    void setupOrg() {
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            // 1. Department head
            defaultIdentity.registerDepartmentManager("DEPT_RD", "Director_David");

            // 2. General Manager / CEO
            defaultIdentity.registerDepartmentManager("DEPT_GM", "CEO_Steve");

            // 3. User profile
            defaultIdentity.registerUserProfile(UserProfile.builder("Alice")
                    .departmentId("DEPT_RD")
                    .postCode("SENIOR_DEV")
                    .username("爱丽丝")
                    .build());
        }

        // Single workflow node with natural language condition branch
        leaveWorkflow = ProcessDefinition.builder("conditional-leave-workflow")
                .startNode("start")
                .humanNode("leave-approval", h -> h
                        .name("请假审批")
                        .assigneePrompt("如果请假天数大于3天由总经理审批，否则由部门负责人审批")
                )
                .endNode("end")
                .edge("start", "leave-approval")
                .edge("leave-approval", "end")
                .build();
    }

    @Test
    @DisplayName("should accurately isolate cache slots between leaveDays <= 3 and leaveDays > 3 without collision")
    void should_isolate_cache_between_condition_branches() {
        // 1. First Run: Alice requests 1 day leave (<= 3) -> routes to Dept Head Director_David
        String key1 = "LEAVE_001";
        processEngine.start(leaveWorkflow, key1, ProcessContext.builder()
                .initiator("Alice")
                .variable("leaveDays", 1)
                .build());

        List<Task> davidTasks1 = taskService.findPendingTasks("Director_David", List.of());
        assertThat(davidTasks1).anyMatch(t -> key1.equals(t.businessKey()));

        // 2. Second Run: Alice requests 5 days leave (> 3) -> Must NOT hit 1-day cache, routes to CEO_Steve!
        String key2 = "LEAVE_002";
        processEngine.start(leaveWorkflow, key2, ProcessContext.builder()
                .initiator("Alice")
                .variable("leaveDays", 5)
                .build());

        List<Task> ceoTasks1 = taskService.findPendingTasks("CEO_Steve", List.of());
        assertThat(ceoTasks1).anyMatch(t -> key2.equals(t.businessKey()));

        // 3. Third Run: Alice requests 2 days leave (<= 3) -> Hits Branch 1 cache (0 Token to David)!
        String key3 = "LEAVE_003";
        processEngine.start(leaveWorkflow, key3, ProcessContext.builder()
                .initiator("Alice")
                .variable("leaveDays", 2)
                .build());

        List<Task> davidTasks2 = taskService.findPendingTasks("Director_David", List.of());
        assertThat(davidTasks2).anyMatch(t -> key3.equals(t.businessKey()));

        // 4. Fourth Run: Alice requests 10 days leave (> 3) -> Hits Branch 2 cache (0 Token to CEO)!
        String key4 = "LEAVE_004";
        processEngine.start(leaveWorkflow, key4, ProcessContext.builder()
                .initiator("Alice")
                .variable("leaveDays", 10)
                .build());

        List<Task> ceoTasks2 = taskService.findPendingTasks("CEO_Steve", List.of());
        assertThat(ceoTasks2).anyMatch(t -> key4.equals(t.businessKey()));
    }
}
