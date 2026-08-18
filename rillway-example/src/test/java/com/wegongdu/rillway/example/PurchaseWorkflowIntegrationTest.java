package com.wegongdu.rillway.example;

import com.wegongdu.rillway.audit.event.AuditEvents;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseApplication.class)
class PurchaseWorkflowIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private InMemoryAuditSink auditSink;

    private final ProcessDefinition purchaseDefinition = PurchaseWorkflowFactory.createPurchaseWorkflow();

    @Test
    @DisplayName("should route to manager when amount is below 5000")
    void should_route_to_manager_when_amount_is_below_5000() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("amount", new BigDecimal("3200"))
                .variable("item", "办公桌椅")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("manager-approval");

        // Manager approves
        ApproveDecision managerDecision = ApproveDecision.of(
                Actor.HumanActor.of("manager-001", "DEPARTMENT_MANAGER"),
                "同意采购"
        );
        ProcessInstance completed = processEngine.resume(instance, managerDecision);

        assertThat(completed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(completed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should allow agent to approve with delegated authority for standard amount")
    void should_allow_agent_to_approve_with_delegated_authority() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Bob")
                .variable("amount", new BigDecimal("12000"))
                .variable("item", "研发测试服务器")
                .variable("category", "IT_HARDWARE")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        // Agent should automatically approve and complete
        assertThat(instance.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(instance.currentNodeId()).isEqualTo("end");
        assertThat(instance.history()).anyMatch(h -> "agent-review".equals(h.nodeId()) && h.decision() instanceof ApproveDecision);
    }

    @Test
    @DisplayName("should escalate to general manager when amount exceeds 50000")
    void should_escalate_to_general_manager_when_amount_exceeds_50000() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Charlie")
                .variable("amount", new BigDecimal("85000"))
                .variable("item", "高规格机房核心交换机集群")
                .variable("category", "IT_HARDWARE")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        // Agent should escalate to general manager
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("general-manager-approval");

        // General manager approves
        ApproveDecision gmDecision = ApproveDecision.of(
                Actor.HumanActor.of("gm-001", "GENERAL_MANAGER"),
                "金额较大但确属核心基础设施，同意采购"
        );
        ProcessInstance completed = processEngine.resume(instance, gmDecision);

        assertThat(completed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(completed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should fallback to human procurement manager when agent cannot verify invoice")
    void should_fallback_to_human_when_agent_cannot_decide() {
        ProcessContext context = ProcessContext.builder()
                .initiator("David")
                .variable("amount", new BigDecimal("8000"))
                .variable("item", "定制化展厅屏幕")
                .variable("category", "OFFICE")
                .variable("hasInvoice", false) // Missing invoice
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        // Agent triggers fallback to procurement manager
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("procurement-manager-approval");

        // Procurement manager reviews offline and approves
        ApproveDecision procDecision = ApproveDecision.of(
                Actor.HumanActor.of("pm-001", "PROCUREMENT_MANAGER"),
                "已电话联系供应商补充报价单，核验通过"
        );
        ProcessInstance completed = processEngine.resume(instance, procDecision);

        assertThat(completed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(completed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should reject when purchasing prohibited items")
    void should_reject_prohibited_items() {
        ProcessContext context = ProcessContext.builder()
                .initiator("Eve")
                .variable("amount", new BigDecimal("15000"))
                .variable("item", "PS5 游戏主机及游戏光盘")
                .variable("category", "GAMING")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.REJECTED);
        assertThat(instance.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should record audit events throughout workflow")
    void should_record_audit_events() {
        auditSink.clear();

        ProcessContext context = ProcessContext.builder()
                .initiator("Frank")
                .variable("amount", new BigDecimal("6000"))
                .variable("item", "显示器支架与扩展坞")
                .variable("category", "OFFICE")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, context);
        assertThat(instance.status()).isEqualTo(ProcessStatus.COMPLETED);

        assertThat(auditSink.getEventsOfType(AuditEvents.ProcessStarted.class)).isNotEmpty();
        assertThat(auditSink.getEventsOfType(AuditEvents.NodeEntered.class)).isNotEmpty();
        assertThat(auditSink.getEventsOfType(AuditEvents.AgentInvoked.class)).isNotEmpty();
        assertThat(auditSink.getEventsOfType(AuditEvents.AgentDecisionMade.class)).isNotEmpty();
        assertThat(auditSink.getEventsOfType(AuditEvents.ProcessCompleted.class)).isNotEmpty();
    }
}
