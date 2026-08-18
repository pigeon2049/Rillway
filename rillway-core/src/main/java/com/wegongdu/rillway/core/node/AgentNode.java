package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.model.NodeType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AI Agent takeover node.
 * <p>
 * An AgentNode defines the operational boundary for an AI Agent, including its authority,
 * relevant enterprise policies, allowed decision types, and fallback path.
 */
public record AgentNode(
        String id,
        String name,
        String agentId,
        AgentAuthority authority,
        List<String> policies,
        Set<DecisionType> allowedDecisions,
        String fallbackNodeId,
        Map<DecisionType, String> decisionRoutes,
        String defaultTargetNodeId
) implements Node {

    public AgentNode {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = "Agent Takeover";
        }
        policies = policies != null ? List.copyOf(policies) : Collections.emptyList();
        allowedDecisions = allowedDecisions != null ? Set.copyOf(allowedDecisions) : Collections.emptySet();
        decisionRoutes = decisionRoutes != null ? Map.copyOf(decisionRoutes) : Collections.emptyMap();
    }

    @Override
    public NodeType type() {
        return NodeType.AGENT;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String name;
        private String agentId;
        private AgentAuthority authority = AgentAuthority.DELEGATED;
        private final List<String> policies = new java.util.ArrayList<>();
        private final Set<DecisionType> allowedDecisions = new HashSet<>();
        private String fallbackNodeId;
        private final Map<DecisionType, String> decisionRoutes = new HashMap<>();
        private String defaultTargetNodeId;

        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder authority(AgentAuthority authority) {
            this.authority = authority;
            return this;
        }

        public Builder policies(String... policies) {
            if (policies != null) {
                this.policies.addAll(List.of(policies));
            }
            return this;
        }

        public Builder policies(List<String> policies) {
            if (policies != null) {
                this.policies.addAll(policies);
            }
            return this;
        }

        public Builder allowedDecisions(DecisionType... types) {
            if (types != null) {
                this.allowedDecisions.addAll(List.of(types));
            }
            return this;
        }

        public Builder allowedDecisions(Set<DecisionType> types) {
            if (types != null) {
                this.allowedDecisions.addAll(types);
            }
            return this;
        }

        public Builder fallbackNodeId(String fallbackNodeId) {
            this.fallbackNodeId = fallbackNodeId;
            return this;
        }

        public Builder on(DecisionType decisionType, String targetNodeId) {
            this.decisionRoutes.put(decisionType, targetNodeId);
            return this;
        }

        public Builder defaultTargetNodeId(String defaultTargetNodeId) {
            this.defaultTargetNodeId = defaultTargetNodeId;
            return this;
        }

        public AgentNode build() {
            return new AgentNode(
                    id,
                    name,
                    agentId,
                    authority,
                    policies,
                    allowedDecisions,
                    fallbackNodeId,
                    decisionRoutes,
                    defaultTargetNodeId
            );
        }
    }
}
