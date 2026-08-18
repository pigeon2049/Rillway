package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.model.BindingConfig;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
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

@SpringBootTest(classes = PurchaseApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_zerocode_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ZeroCodeBindingIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BindingConfigRepository bindingConfigRepository;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupDatabaseAndConfig() {
        // 1. Create a simulated business order table in the application database
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS biz_purchase_order (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "title VARCHAR(128), " +
                "amount DECIMAL(10,2), " +
                "status VARCHAR(32)" +
                ")");

        // 2. Configure binding mapping: business_type 'purchase_order' -> table 'biz_purchase_order' -> status 'status'
        BindingConfig config = BindingConfig.of(
                "cfg_po_01",
                "purchase_order",
                "dynamic-approval-workflow",
                "biz_purchase_order",
                "status",
                "APPROVED",
                "REJECTED"
        );
        bindingConfigRepository.save(config);

        // 3. Configure organization hierarchy in IdentityService
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            // Employee Alice's direct leader is Bob
            defaultIdentity.registerDirectLeader("Alice", "Manager_Bob");
        }
    }

    @Test
    @DisplayName("should automatically resolve leader via expression and auto-update entity status upon approval")
    void should_resolve_leader_and_auto_update_entity_status() {
        String orderId = "PO_20260818_9988";

        // Insert initial business record with DRAFT status
        jdbcTemplate.update("INSERT INTO biz_purchase_order (id, title, amount, status) VALUES (?, ?, ?, ?)",
                orderId, "高性能交换机采购", new BigDecimal("4500"), "DRAFT");

        // Define workflow where human approval assignee is dynamically set to #{leader(initiator)}
        ProcessDefinition definition = ProcessDefinition.builder("dynamic-approval-workflow")
                .startNode("start")
                .humanNode("leader-approval", h -> h
                        .name("直属领导审批")
                        .assigneeUser("#{leader(initiator)}")
                )
                .endNode("end")
                .edge("start", "leader-approval")
                .edge("leader-approval", "end")
                .build();

        // 1. Employee Alice submits order and starts workflow
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("amount", new BigDecimal("4500"))
                .build();

        processEngine.start(definition, "purchase_order:" + orderId, context);

        // 2. Query pending tasks for Manager_Bob (automatically resolved from #{leader(initiator)})
        List<Task> bobTasks = taskService.findPendingTasks("Manager_Bob", List.of());
        assertThat(bobTasks).anyMatch(t -> ("purchase_order:" + orderId).equals(t.businessKey()));

        Task targetTask = bobTasks.stream()
                .filter(t -> ("purchase_order:" + orderId).equals(t.businessKey()))
                .findFirst()
                .orElseThrow();

        assertThat(targetTask.assigneeUser()).isEqualTo("Manager_Bob");
        assertThat(targetTask.status()).isEqualTo(TaskStatus.PENDING);

        // 3. Manager_Bob completes the approval task
        taskService.completeTask(
                targetTask.id(),
                ApproveDecision.of(Actor.HumanActor.of("Manager_Bob"), "核实预算充足，同意采购")
        );

        // 4. Verify that the business table status was AUTOMATICALLY updated to 'APPROVED' without any custom listener!
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM biz_purchase_order WHERE id = ?",
                String.class,
                orderId
        );

        assertThat(finalStatus).isEqualTo("APPROVED");
    }
}
