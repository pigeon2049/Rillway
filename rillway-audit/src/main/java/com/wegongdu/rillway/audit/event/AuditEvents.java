package com.wegongdu.rillway.audit.event;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.NodeType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete audit events.
 */
public final class AuditEvents {

    private AuditEvents() {}

    public record ProcessStarted(
            String eventId,
            String processInstanceId,
            String definitionId,
            String startNodeId,
            String initiator,
            ProcessContext context,
            Instant timestamp
    ) implements AuditEvent {
        public ProcessStarted {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "PROCESS_STARTED";
        }
    }

    public record NodeEntered(
            String eventId,
            String processInstanceId,
            String definitionId,
            String nodeId,
            String nodeName,
            NodeType nodeType,
            Instant timestamp
    ) implements AuditEvent {
        public NodeEntered {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "NODE_ENTERED";
        }
    }

    public record NodeCompleted(
            String eventId,
            String processInstanceId,
            String definitionId,
            String nodeId,
            String nodeName,
            NodeType nodeType,
            Actor actor,
            Decision decision,
            Instant timestamp
    ) implements AuditEvent {
        public NodeCompleted {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "NODE_COMPLETED";
        }
    }

    public record DecisionMade(
            String eventId,
            String processInstanceId,
            String definitionId,
            String nodeId,
            Actor actor,
            Decision decision,
            Instant timestamp
    ) implements AuditEvent {
        public DecisionMade {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "DECISION_MADE";
        }
    }

    public record AgentInvoked(
            String eventId,
            String processInstanceId,
            String definitionId,
            String nodeId,
            String agentId,
            AgentAuthority authority,
            Instant timestamp
    ) implements AuditEvent {
        public AgentInvoked {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "AGENT_INVOKED";
        }
    }

    public record AgentDecisionMade(
            String eventId,
            String processInstanceId,
            String definitionId,
            String nodeId,
            String agentId,
            AgentAuthority authority,
            Decision decision,
            Instant timestamp
    ) implements AuditEvent {
        public AgentDecisionMade {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "AGENT_DECISION_MADE";
        }
    }

    public record ProcessCompleted(
            String eventId,
            String processInstanceId,
            String definitionId,
            String finalNodeId,
            boolean isSuccess,
            Instant timestamp
    ) implements AuditEvent {
        public ProcessCompleted {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "PROCESS_COMPLETED";
        }
    }

    public record ProcessFailed(
            String eventId,
            String processInstanceId,
            String definitionId,
            String failedNodeId,
            String errorMessage,
            Instant timestamp
    ) implements AuditEvent {
        public ProcessFailed {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (timestamp == null) timestamp = Instant.now();
        }

        @Override
        public String eventType() {
            return "PROCESS_FAILED";
        }
    }
}
