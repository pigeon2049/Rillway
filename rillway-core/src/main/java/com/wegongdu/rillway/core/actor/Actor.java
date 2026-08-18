package com.wegongdu.rillway.core.actor;

import java.util.Objects;

/**
 * Represents the entity that performed a decision or action in the workflow.
 */
public sealed interface Actor permits Actor.HumanActor, Actor.RuleActor, Actor.AgentActor {

    String identifier();

    String displayName();

    record HumanActor(String userId, String role, String name) implements Actor {
        public HumanActor {
            Objects.requireNonNull(userId, "userId must not be null");
        }

        @Override
        public String identifier() {
            return "human:" + userId;
        }

        @Override
        public String displayName() {
            return name != null && !name.isBlank() ? name : userId;
        }

        public static HumanActor of(String userId) {
            return new HumanActor(userId, null, userId);
        }

        public static HumanActor of(String userId, String role) {
            return new HumanActor(userId, role, userId);
        }
    }

    record RuleActor(String ruleId, String ruleName) implements Actor {
        public RuleActor {
            Objects.requireNonNull(ruleId, "ruleId must not be null");
        }

        @Override
        public String identifier() {
            return "rule:" + ruleId;
        }

        @Override
        public String displayName() {
            return ruleName != null && !ruleName.isBlank() ? ruleName : ruleId;
        }

        public static RuleActor of(String ruleId) {
            return new RuleActor(ruleId, ruleId);
        }

        public static RuleActor of(String ruleId, String ruleName) {
            return new RuleActor(ruleId, ruleName);
        }
    }

    record AgentActor(String agentId, String model, String version) implements Actor {
        public AgentActor {
            Objects.requireNonNull(agentId, "agentId must not be null");
        }

        @Override
        public String identifier() {
            return "agent:" + agentId;
        }

        @Override
        public String displayName() {
            return "Agent [" + agentId + "]";
        }

        public static AgentActor of(String agentId) {
            return new AgentActor(agentId, null, null);
        }
    }
}
