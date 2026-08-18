package com.wegongdu.rillway.core.instance;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.model.NodeType;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Audit record of a single node's execution and its decision.
 */
public record ExecutionRecord(
        String nodeId,
        String nodeName,
        NodeType nodeType,
        Actor actor,
        Decision decision,
        Instant enteredAt,
        Instant completedAt,
        String errorMessage
) implements Serializable {

    public ExecutionRecord {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        if (enteredAt == null) {
            enteredAt = Instant.now();
        }
    }

    public static ExecutionRecord of(String nodeId, String nodeName, NodeType nodeType, Instant enteredAt) {
        return new ExecutionRecord(nodeId, nodeName, nodeType, null, null, enteredAt, null, null);
    }

    public ExecutionRecord completed(Actor actor, Decision decision) {
        return new ExecutionRecord(nodeId, nodeName, nodeType, actor, decision, enteredAt, Instant.now(), null);
    }

    public ExecutionRecord failed(String errorMessage) {
        return new ExecutionRecord(nodeId, nodeName, nodeType, null, null, enteredAt, Instant.now(), errorMessage);
    }
}
