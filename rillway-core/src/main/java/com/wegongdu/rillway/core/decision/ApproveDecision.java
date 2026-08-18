package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;
import java.util.Objects;

/**
 * Approve decision.
 */
public record ApproveDecision(
        Actor actor,
        String reason,
        Instant timestamp,
        AgentDecisionExplanation explanation
) implements Decision {

    public ApproveDecision {
        Objects.requireNonNull(actor, "actor must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public DecisionType type() {
        return DecisionType.APPROVE;
    }

    public static ApproveDecision of(Actor actor, String reason) {
        return new ApproveDecision(actor, reason, Instant.now(), null);
    }

    public static ApproveDecision of(Actor actor, String reason, AgentDecisionExplanation explanation) {
        return new ApproveDecision(actor, reason, Instant.now(), explanation);
    }
}
