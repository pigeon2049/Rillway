package com.wegongdu.rillway.core.instance;

import com.wegongdu.rillway.core.decision.Decision;
import java.io.Serializable;
import java.util.Objects;

/**
 * Result of executing a specific node in the workflow.
 */
public record NodeExecutionResult(
        Status status,
        Decision decision,
        String nextNodeId,
        String errorMessage
) implements Serializable {

    public enum Status {
        /** The node completed and the workflow should advance to nextNodeId */
        ADVANCE,
        /** The node requires human action or external input, workflow is paused here */
        SUSPEND,
        /** The workflow reached an end node */
        COMPLETE,
        /** Node execution failed */
        FAILED
    }

    public static NodeExecutionResult advance(String nextNodeId, Decision decision) {
        Objects.requireNonNull(nextNodeId, "nextNodeId must not be null");
        return new NodeExecutionResult(Status.ADVANCE, decision, nextNodeId, null);
    }

    public static NodeExecutionResult suspend() {
        return new NodeExecutionResult(Status.SUSPEND, null, null, null);
    }

    public static NodeExecutionResult complete(Decision decision) {
        return new NodeExecutionResult(Status.COMPLETE, decision, null, null);
    }

    public static NodeExecutionResult failed(String errorMessage) {
        return new NodeExecutionResult(Status.FAILED, null, null, errorMessage);
    }
}
