package com.wegongdu.rillway.example.agent;

import com.wegongdu.rillway.agent.model.AgentContext;
import com.wegongdu.rillway.agent.model.AgentDecision;
import com.wegongdu.rillway.agent.spi.Agent;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.AgentDecisionExplanation;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.EscalateDecision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import java.math.BigDecimal;
import java.util.List;

/**
 * Intelligent Agent responsible for reviewing enterprise purchase applications.
 */
public class PurchaseReviewAgent implements Agent {

    public static final String ID = "purchase-review-agent";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public AgentDecision decide(AgentContext context) {
        BigDecimal amount = context.variables().getDecimal("amount");
        Boolean hasInvoice = context.variables().getBoolean("hasInvoice");
        String category = context.variables().getString("category");

        if (amount == null) {
            return AgentDecision.fallback("Purchase amount is missing in form context");
        }

        // 1. If invoice or quotation is missing, fallback to human procurement manager
        if (Boolean.FALSE.equals(hasInvoice)) {
            return AgentDecision.fallback("Invoice/Quotation not attached, requires manual procurement verification");
        }

        // 2. If amount exceeds delegated threshold (> 50,000 RMB), escalate to General Manager
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            AgentDecisionExplanation explanation = AgentDecisionExplanation.builder()
                    .reasoningSummary("Purchase amount ¥" + amount + " exceeds delegated authority limit (¥50,000)")
                    .evidence("amount", amount)
                    .evidence("category", category)
                    .policyReferences("PURCHASE_POLICY_2026#Article4.2")
                    .confidence(0.98)
                    .build();

            return AgentDecision.of(EscalateDecision.of(
                    Actor.AgentActor.of(ID),
                    "GENERAL_MANAGER",
                    "Exceeds ¥50,000 threshold, escalating to General Manager for sign-off",
                    explanation
            ));
        }

        // 3. Reject prohibited items
        if ("GAMING".equalsIgnoreCase(category) || "LUXURY".equalsIgnoreCase(category)) {
            AgentDecisionExplanation explanation = AgentDecisionExplanation.builder()
                    .reasoningSummary("Purchase category [" + category + "] is listed on company prohibited procurement items")
                    .policyReferences("PURCHASE_POLICY_2026#Article8.1")
                    .confidence(1.0)
                    .build();

            return AgentDecision.of(RejectDecision.of(
                    Actor.AgentActor.of(ID),
                    "Non-compliant category: " + category,
                    explanation
            ));
        }

        // 4. Autonomous / Delegated approval within policy
        AgentDecisionExplanation explanation = AgentDecisionExplanation.builder()
                .reasoningSummary("Amount ¥" + amount + " compliant with standard IT/office procurement policy")
                .evidence("amount", amount)
                .evidence("hasInvoice", true)
                .policyReferences("PURCHASE_POLICY_2026#Article3.1")
                .confidence(0.95)
                .build();

        return AgentDecision.of(ApproveDecision.of(
                Actor.AgentActor.of(ID),
                "Approved within standard procurement policy bounds",
                explanation
        ));
    }
}
