package com.wegongdu.rillway.core.decision;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.model.DecisionType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request information decision asking for additional details or documents.
 */
public record RequestInformationDecision(
        Actor actor,
        List<String> requestedFields,
        String reason,
        Instant timestamp,
        AgentDecisionExplanation explanation
) implements Decision {

    public RequestInformationDecision {
        Objects.requireNonNull(actor, "actor must not be null");
        requestedFields = requestedFields != null ? List.copyOf(requestedFields) : Collections.emptyList();
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public DecisionType type() {
        return DecisionType.REQUEST_INFORMATION;
    }

    public static RequestInformationDecision of(Actor actor, List<String> requestedFields, String reason) {
        return new RequestInformationDecision(actor, requestedFields, reason, Instant.now(), null);
    }
}
