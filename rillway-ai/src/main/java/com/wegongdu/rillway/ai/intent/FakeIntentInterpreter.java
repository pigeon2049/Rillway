package com.wegongdu.rillway.ai.intent;

import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import java.math.BigDecimal;

/**
 * Fake/mock implementation of IntentInterpreter for deterministic tests and offline demonstrations.
 */
public class FakeIntentInterpreter implements IntentInterpreter {

    @Override
    public ProcessDefinition interpret(ProcessIntent intent) {
        String text = intent.naturalLanguage();

        // Template generator for purchase approval workflow
        return ProcessDefinition.builder("purchase-process-generated")
                .name("智能采购审批流 (AI Generated)")
                .description("Generated from intent: " + text)
                .startNode("start")
                .ruleNode("amount-check", rule -> rule
                        .name("金额阈值初筛")
                        .when("低于5000由经理审批", ctx -> {
                            BigDecimal amt = ctx.getDecimal("amount");
                            return amt != null && amt.compareTo(new BigDecimal("5000")) < 0;
                        }, "manager-approval")
                        .otherwise("agent-review")
                )
                .humanNode("manager-approval", human -> human
                        .name("直属经理审批")
                        .assigneeRole("DEPARTMENT_MANAGER")
                )
                .agentNode("agent-review", agent -> agent
                        .name("采购智能审核")
                        .agentId("purchase-review-agent")
                        .authority(AgentAuthority.DELEGATED)
                        .policies("PURCHASE_POLICY_2026", "INVOICE_STANDARD")
                        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT, DecisionType.ESCALATE)
                        .fallbackNodeId("procurement-manager-approval")
                        .on(DecisionType.APPROVE, "end")
                        .on(DecisionType.REJECT, "end")
                        .on(DecisionType.ESCALATE, "general-manager-approval")
                )
                .humanNode("procurement-manager-approval", human -> human
                        .name("采购经理人工复核")
                        .assigneeRole("PROCUREMENT_MANAGER")
                )
                .humanNode("general-manager-approval", human -> human
                        .name("总经理审批")
                        .assigneeRole("GENERAL_MANAGER")
                )
                .endNode("end", "审批结束")
                .edge("start", "amount-check")
                .edge("manager-approval", "end")
                .edge("procurement-manager-approval", "end")
                .edge("general-manager-approval", "end")
                .build();
    }
}
