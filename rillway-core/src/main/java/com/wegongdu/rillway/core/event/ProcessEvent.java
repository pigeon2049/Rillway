package com.wegongdu.rillway.core.event;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.model.NodeType;
import com.wegongdu.rillway.core.model.ProcessStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Root interface for all workflow process lifecycle events.
 */
public sealed interface ProcessEvent permits
        ProcessEvent.ProcessStartedEvent,
        ProcessEvent.NodeEnteredEvent,
        ProcessEvent.NodeCompletedEvent,
        ProcessEvent.ProcessCompletedEvent,
        ProcessEvent.ProcessFailedEvent {

    String eventId();

    String processInstanceId();

    String definitionId();

    String businessKey();

    Instant timestamp();

    /**
     * Fired when a process instance is initialized and started.
     */
    record ProcessStartedEvent(
            String eventId,
            String processInstanceId,
            String definitionId,
            String businessKey,
            String startNodeId,
            String initiator,
            ProcessContext context,
            Instant timestamp
    ) implements ProcessEvent {
        public ProcessStartedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }
    }

    /**
     * Fired when execution enters a workflow node.
     */
    record NodeEnteredEvent(
            String eventId,
            String processInstanceId,
            String definitionId,
            String businessKey,
            String nodeId,
            String nodeName,
            NodeType nodeType,
            String assigneeRole,
            String assigneeUser,
            Instant timestamp
    ) implements ProcessEvent {
        public NodeEnteredEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }
    }

    /**
     * Fired when a workflow node finishes execution.
     */
    record NodeCompletedEvent(
            String eventId,
            String processInstanceId,
            String definitionId,
            String businessKey,
            String nodeId,
            String nodeName,
            NodeType nodeType,
            Actor actor,
            Decision decision,
            Instant timestamp
    ) implements ProcessEvent {
        public NodeCompletedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }
    }

    /**
     * Fired when a process instance finishes (either APPROVED/COMPLETED or REJECTED).
     */
    record ProcessCompletedEvent(
            String eventId,
            String processInstanceId,
            String definitionId,
            String businessKey,
            String finalNodeId,
            boolean isSuccess,
            ProcessStatus finalStatus,
            Instant timestamp
    ) implements ProcessEvent {
        public ProcessCompletedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }
    }

    /**
     * Fired when a process instance encounters an unhandled failure or error.
     */
    record ProcessFailedEvent(
            String eventId,
            String processInstanceId,
            String definitionId,
            String businessKey,
            String failedNodeId,
            String errorMessage,
            Instant timestamp
    ) implements ProcessEvent {
        public ProcessFailedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }
    }
}
