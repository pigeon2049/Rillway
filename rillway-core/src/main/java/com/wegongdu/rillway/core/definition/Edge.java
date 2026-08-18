package com.wegongdu.rillway.core.definition;

import com.wegongdu.rillway.core.model.DecisionType;
import java.io.Serializable;
import java.util.Objects;

/**
 * Directed edge connecting two nodes in a workflow definition.
 */
public record Edge(
        String id,
        String sourceNodeId,
        String targetNodeId,
        DecisionType onDecision,
        String conditionDescription
) implements Serializable {

    public Edge {
        Objects.requireNonNull(sourceNodeId, "sourceNodeId must not be null");
        Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        if (id == null || id.isBlank()) {
            id = sourceNodeId + "->" + targetNodeId + (onDecision != null ? "[" + onDecision + "]" : "");
        }
    }

    public static Edge of(String sourceNodeId, String targetNodeId) {
        return new Edge(null, sourceNodeId, targetNodeId, null, null);
    }

    public static Edge of(String sourceNodeId, String targetNodeId, DecisionType onDecision) {
        return new Edge(null, sourceNodeId, targetNodeId, onDecision, null);
    }

    public static Edge of(String sourceNodeId, String targetNodeId, DecisionType onDecision, String conditionDescription) {
        return new Edge(null, sourceNodeId, targetNodeId, onDecision, conditionDescription);
    }
}
