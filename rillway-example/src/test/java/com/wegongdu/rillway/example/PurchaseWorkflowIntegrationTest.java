package com.wegongdu.rillway.example;

import com.wegongdu.rillway.audit.event.AuditEvent;
import com.wegongdu.rillway.audit.event.AuditEvents;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.audit.sink.InMemoryAuditSink;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.example.workflow.PurchaseWorkflowFactory;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RillwayExampleApplication.class)
class PurchaseWorkflowIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private AuditSink auditSink;

    private final ProcessDefinition purchaseDefinition = PurchaseWorkflowFactory.createPurchaseWorkflow();

    @Test
    @DisplayName("should route to manager approval when amount is below 5000")
    void should_route_to_manager_when_amount_is_below_5000() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("amount", new BigDecimal("3000"))
                .variable("item", "办公椅")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("manager-approval");

        // Human manager approves
        ApproveDecision decision = ApproveDecision.of(
                Actor.HumanActor.of("manager-01", "DEPARTMENT_MANAGER"),
                "同意采购"
        );
        ProcessInstance resumed = processEngine.resume(instance, decision);

        assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(resumed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should automatically approve via agent when amount is between 5000 and 50000 with invoice")
    void should_allow_agent_to_approve_with_delegated_authority() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Bob")
                .variable("amount", new BigDecimal("12000"))
                .variable("item", "研发服务器")
                .variable("category", "IT_HARDWARE")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        // Agent should have approved and moved directly to end
        assertThat(instance.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(instance.currentNodeId()).isEqualTo("end");
        assertThat(instance.history()).isNotEmpty();
    }

    @Test
    @DisplayName("should escalate to general manager when amount exceeds 50000")
    void should_escalate_to_general_manager_when_amount_exceeds_50000() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Charlie")
                .variable("amount", new BigDecimal("80000"))
                .variable("item", "机房高可用集群")
                .variable("category", "INFRASTRUCTURE")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("general-manager-approval");

        // GM approves
        ApproveDecision gmDecision = ApproveDecision.of(
                Actor.HumanActor.of("gm-01", "GENERAL_MANAGER"),
                "战略采购，批准"
        );
        ProcessInstance resumed = processEngine.resume(instance, gmDecision);

        assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(resumed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should reject prohibited purchase category immediately via agent")
    void should_reject_prohibited_items() {
        ProcessContext context = ProcessContext.builder()
                .initiator("David")
                .variable("amount", new BigDecimal("8000")) // >= 5000 routed to agent
                .variable("item", "高档烟酒礼品")
                .variable("category", "LUXURY_GIFT")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.REJECTED);
        assertThat(instance.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should fallback to human manager when agent cannot decide (e.g. missing invoice)")
    void should_fallback_to_human_when_agent_cannot_decide() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Eva")
                .variable("amount", new BigDecimal("15000"))
                .variable("item", "办公笔记本")
                .variable("category", "IT_HARDWARE")
                .variable("hasInvoice", false) // Missing invoice triggers agent fallback
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        // Should fallback and pause at procurement-manager-approval
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("procurement-manager-approval");
    }

    @Test
    @DisplayName("should record audit events throughout the workflow execution")
    void should_record_audit_events() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("amount", new BigDecimal("3000"))
                .variable("item", "测试显示器")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance).isNotNull();

        List<AuditEvent> events = getAuditEvents();
        assertThat(events).isNotEmpty();
        assertThat(events).anyMatch(e -> e instanceof AuditEvents.ProcessStarted);
        assertThat(events).anyMatch(e -> e instanceof AuditEvents.NodeEntered ne && "amount-check".equals(ne.nodeId()));
    }

    private List<AuditEvent> getAuditEvents() {
        if (auditSink instanceof InMemoryAuditSink inMemory) {
            return inMemory.getEvents();
        }
        if (auditSink instanceof com.wegongdu.rillway.autoconfigure.binding.EntityStatusAutoUpdater autoUpdater) {
            if (autoUpdater.getDelegateSink() instanceof InMemoryAuditSink inMemory) {
                return inMemory.getEvents();
            }
        }
        return List.of();
    }
}
