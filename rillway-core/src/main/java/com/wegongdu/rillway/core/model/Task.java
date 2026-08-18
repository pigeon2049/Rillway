package com.wegongdu.rillway.core.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain representation of a human task waiting for user action.
 */
public record Task(
        String id,
        String processInstanceId,
        String businessKey,
        String definitionId,
        String nodeId,
        String nodeName,
        String assigneeUser,
        String assigneeRole,
        List<String> candidateUsers,
        List<String> candidateRoles,
        TaskStatus status,
        Instant createdAt,
        Instant completedAt
) implements Serializable {

    public Task {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(processInstanceId, "processInstanceId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        candidateUsers = candidateUsers != null ? List.copyOf(candidateUsers) : Collections.emptyList();
        candidateRoles = candidateRoles != null ? List.copyOf(candidateRoles) : Collections.emptyList();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static Task createPending(
            String processInstanceId,
            String businessKey,
            String definitionId,
            String nodeId,
            String nodeName,
            String assigneeUser,
            String assigneeRole,
            List<String> candidateUsers,
            List<String> candidateRoles
    ) {
        return new Task(
                UUID.randomUUID().toString(),
                processInstanceId,
                businessKey,
                definitionId,
                nodeId,
                nodeName != null ? nodeName : nodeId,
                assigneeUser,
                assigneeRole,
                candidateUsers,
                candidateRoles,
                TaskStatus.PENDING,
                Instant.now(),
                null
        );
    }

    public Task complete() {
        return new Task(
                id,
                processInstanceId,
                businessKey,
                definitionId,
                nodeId,
                nodeName,
                assigneeUser,
                assigneeRole,
                candidateUsers,
                candidateRoles,
                TaskStatus.COMPLETED,
                createdAt,
                Instant.now()
        );
    }

    public Task cancel() {
        return new Task(
                id,
                processInstanceId,
                businessKey,
                definitionId,
                nodeId,
                nodeName,
                assigneeUser,
                assigneeRole,
                candidateUsers,
                candidateRoles,
                TaskStatus.CANCELLED,
                createdAt,
                Instant.now()
        );
    }
}
