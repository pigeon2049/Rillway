package com.wegongdu.rillway.example.workflow;

import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.example.agent.PurchaseReviewAgent;
import java.math.BigDecimal;

/**
 * Factory for creating standard purchase approval workflow definitions.
 */
public final class PurchaseWorkflowFactory {

    public static final String PROCESS_ID = "purchase-approval-workflow";

    private PurchaseWorkflowFactory() {}

    public static ProcessDefinition createPurchaseWorkflow() {
        return ProcessDefinition.builder(PROCESS_ID)
                .name("企业智能采购合规审批流程")
                .description("5000元以下经理审批；5000元以上由采购Agent审核；>50000元升级总经理；缺失发票降级采购经理")
                .startNode("start", "提交申请")
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
                        .agentId(PurchaseReviewAgent.ID)
                        .authority(AgentAuthority.DELEGATED)
                        .policies("PURCHASE_POLICY_2026", "INVOICE_STANDARD")
                        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT, DecisionType.ESCALATE)
                        .fallbackNodeId("procurement-manager-approval")
                        .on(DecisionType.APPROVE, "end")
                        .on(DecisionType.REJECT, "end")
                        .on(DecisionType.ESCALATE, "general-manager-approval")
                )
                .humanNode("procurement-manager-approval", human -> human
                        .name("采购经理人工复核 (Fallback)")
                        .assigneeRole("PROCUREMENT_MANAGER")
                )
                .humanNode("general-manager-approval", human -> human
                        .name("总经理审批 (Escalated)")
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
