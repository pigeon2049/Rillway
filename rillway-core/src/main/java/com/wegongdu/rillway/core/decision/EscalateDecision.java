package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;
import java.util.Objects;

/**
 * Escalate decision forwarding the workflow to a higher authority.
 */
public record EscalateDecision(
        Actor actor,
        String escalateToRole,
        String reason,
        Instant timestamp,
        AgentDecisionExplanation explanation
) implements Decision {

    public EscalateDecision {
        Objects.requireNonNull(actor, "actor must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public DecisionType type() {
        return DecisionType.ESCALATE;
    }

    public static EscalateDecision of(Actor actor, String escalateToRole, String reason) {
        return new EscalateDecision(actor, escalateToRole, reason, Instant.now(), null);
    }

    public static EscalateDecision of(Actor actor, String escalateToRole, String reason, AgentDecisionExplanation explanation) {
        return new EscalateDecision(actor, escalateToRole, reason, Instant.now(), explanation);
    }
}
