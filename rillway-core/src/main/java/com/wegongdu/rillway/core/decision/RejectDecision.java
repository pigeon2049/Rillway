package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;
import java.util.Objects;

/**
 * Reject decision.
 */
public record RejectDecision(
        Actor actor,
        String reason,
        Instant timestamp,
        AgentDecisionExplanation explanation
) implements Decision {

    public RejectDecision {
        Objects.requireNonNull(actor, "actor must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public DecisionType type() {
        return DecisionType.REJECT;
    }

    public static RejectDecision of(Actor actor, String reason) {
        return new RejectDecision(actor, reason, Instant.now(), null);
    }

    public static RejectDecision of(Actor actor, String reason, AgentDecisionExplanation explanation) {
        return new RejectDecision(actor, reason, Instant.now(), explanation);
    }
}
