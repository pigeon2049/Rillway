package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;
import java.util.Objects;

/**
 * Route decision determining the next target node ID.
 */
public record RouteDecision(
        Actor actor,
        String targetNodeId,
        String reason,
        Instant timestamp,
        AgentDecisionExplanation explanation
) implements Decision {

    public RouteDecision {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public DecisionType type() {
        return DecisionType.ROUTE;
    }

    public static RouteDecision of(Actor actor, String targetNodeId, String reason) {
        return new RouteDecision(actor, targetNodeId, reason, Instant.now(), null);
    }

    public static RouteDecision of(Actor actor, String targetNodeId, String reason, AgentDecisionExplanation explanation) {
        return new RouteDecision(actor, targetNodeId, reason, Instant.now(), explanation);
    }
}
