package com.wegongdu.rillway.agent.guard;

import com.wegongdu.rillway.agent.exception.UnauthorizedAgentDecisionException;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.node.AgentNode;

/**
 * Validates whether an Agent's decision adheres to its node-level authority and boundary rules.
 */
public class AgentAuthorityGuard {

    public void validateDecision(AgentNode node, Decision decision) {
        if (node == null || decision == null) {
            return;
        }

        String agentId = node.agentId();
        DecisionType decisionType = decision.type();

        // 1. Check allowed decision types
        if (!node.allowedDecisions().contains(decisionType)) {
            throw new UnauthorizedAgentDecisionException(
                    agentId,
                    node.id(),
                    String.format("Decision type [%s] is not permitted by node configuration (allowed: %s)",
                            decisionType, node.allowedDecisions())
            );
        }

        // 2. Check AgentAuthority constraints
        AgentAuthority authority = node.authority();
        if (authority == AgentAuthority.ADVISORY) {
            // ADVISORY cannot directly make terminal or state-changing APPROVE/REJECT decisions
            if (decisionType == DecisionType.APPROVE || decisionType == DecisionType.REJECT) {
                throw new UnauthorizedAgentDecisionException(
                        agentId,
                        node.id(),
                        "Agent has ADVISORY authority only and cannot independently issue APPROVE or REJECT decisions"
                );
            }
        }
    }
}
