package com.wegongdu.rillway.agent.model;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.node.AgentNode;
import com.wegongdu.rillway.policy.model.PolicyDocument;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Context provided to an Agent when invoked at an AgentNode.
 */
public record AgentContext(
        ProcessInstance process,
        AgentNode node,
        ProcessContext variables,
        List<PolicyDocument> availablePolicies,
        Set<DecisionType> allowedDecisions,
        AgentAuthority authority
) implements Serializable {

    public AgentContext {
        Objects.requireNonNull(process, "process must not be null");
        Objects.requireNonNull(node, "node must not be null");
        if (variables == null) {
            variables = process.context();
        }
        availablePolicies = availablePolicies != null ? List.copyOf(availablePolicies) : Collections.emptyList();
        allowedDecisions = allowedDecisions != null ? Set.copyOf(allowedDecisions) : Collections.emptySet();
        if (authority == null) {
            authority = node.authority();
        }
    }

    public static AgentContext of(
            ProcessInstance process,
            AgentNode node,
            List<PolicyDocument> availablePolicies
    ) {
        return new AgentContext(
                process,
                node,
                process.context(),
                availablePolicies,
                node.allowedDecisions(),
                node.authority()
        );
    }
}
