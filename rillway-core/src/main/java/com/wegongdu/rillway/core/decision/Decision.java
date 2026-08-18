package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;

/**
 * Sealed interface representing a unified decision outcome across Human, Rule, and Agent actors.
 */
public sealed interface Decision permits
        ApproveDecision,
        RejectDecision,
        RouteDecision,
        RequestInformationDecision,
        EscalateDecision {

    DecisionType type();

    Actor actor();

    String reason();

    Instant timestamp();

    AgentDecisionExplanation explanation();
}
