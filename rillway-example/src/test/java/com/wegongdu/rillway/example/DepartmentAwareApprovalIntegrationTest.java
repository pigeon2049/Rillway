package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RillwayExampleApplication.class)
class DepartmentAwareApprovalIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    private ProcessDefinition departmentWorkflow;

    @BeforeEach
    void setupOrganizationAndWorkflow() {
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            // 1. Configure department managers
            defaultIdentity.registerDepartmentManager("DEPT_RD", "Director_David");
            defaultIdentity.registerDepartmentManager("DEPT_SALES", "Director_Sarah");

            // 2. Configure user profiles with organizational details
            defaultIdentity.registerUserProfile(UserProfile.builder("Alice")
                    .username("爱丽丝")
                    .departmentId("DEPT_RD")
                    .departmentName("核心研发部")
                    .postCode("DEV_LEAD")
                    .build());

            defaultIdentity.registerUserProfile(UserProfile.builder("Frank")
                    .username("弗兰克")
                    .departmentId("DEPT_SALES")
                    .departmentName("华东销售部")
                    .postCode("SALES_EXECUTIVE")
                    .build());
        }

        // 3. Define single workflow where approval is specified in natural language prompt
        departmentWorkflow = ProcessDefinition.builder("dept-aware-workflow")
                .startNode("start")
                .humanNode("dept-manager-approval", h -> h
                        .name("部门负责人审批")
                        .assigneePrompt("申请人所在部门负责人审批")
                )
                .endNode("end")
                .edge("start", "dept-manager-approval")
                .edge("dept-manager-approval", "end")
                .build();
    }

    @Test
    @DisplayName("should route to R&D Director David when R&D employee Alice submits")
    void should_route_to_rd_director_for_rd_employee() {
        String businessKey = "EXPENSE_RD_001";
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice") // R&D department
                .variable("amount", new BigDecimal("1800"))
                .build();

        processEngine.start(departmentWorkflow, businessKey, context);

        // Should automatically route to R&D Director David
        List<Task> davidTasks = taskService.findPendingTasks("Director_David", List.of());
        assertThat(davidTasks).anyMatch(t -> businessKey.equals(t.businessKey()) && t.status() == TaskStatus.PENDING);
    }

    @Test
    @DisplayName("should route to Sales Director Sarah when Sales employee Frank submits")
    void should_route_to_sales_director_for_sales_employee() {
        String businessKey = "EXPENSE_SALES_002";
        ProcessContext context = ProcessContext.builder()
                .initiator("Frank") // Sales department
                .variable("amount", new BigDecimal("3200"))
                .build();

        processEngine.start(departmentWorkflow, businessKey, context);

        // Should automatically route to Sales Director Sarah
        List<Task> sarahTasks = taskService.findPendingTasks("Director_Sarah", List.of());
        assertThat(sarahTasks).anyMatch(t -> businessKey.equals(t.businessKey()) && t.status() == TaskStatus.PENDING);
    }
}
