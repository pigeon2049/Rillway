package com.wegongdu.rillway.core.instance;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.model.ProcessStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a running or completed process instance.
 */
public record ProcessInstance(
        String id,
        String definitionId,
        ProcessStatus status,
        String currentNodeId,
        ProcessContext context,
        List<ExecutionRecord> history,
        Instant startedAt,
        Instant completedAt,
        String errorMessage
) implements Serializable {

    public ProcessInstance {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(definitionId, "definitionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (context == null) {
            context = ProcessContext.empty();
        }
        history = history != null ? List.copyOf(history) : Collections.emptyList();
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public static ProcessInstance create(String definitionId, String startNodeId, ProcessContext context) {
        return new ProcessInstance(
                UUID.randomUUID().toString(),
                definitionId,
                ProcessStatus.RUNNING,
                startNodeId,
                context,
                Collections.emptyList(),
                Instant.now(),
                null,
                null
        );
    }

    public ProcessInstance withStatusAndNode(ProcessStatus newStatus, String newCurrentNodeId) {
        return new ProcessInstance(
                id,
                definitionId,
                newStatus,
                newCurrentNodeId,
                context,
                history,
                startedAt,
                newStatus == ProcessStatus.COMPLETED || newStatus == ProcessStatus.REJECTED || newStatus == ProcessStatus.FAILED ? Instant.now() : completedAt,
                errorMessage
        );
    }

    public ProcessInstance withHistoryRecord(ExecutionRecord record) {
        List<ExecutionRecord> newHistory = new ArrayList<>(this.history);
        newHistory.add(record);
        return new ProcessInstance(
                id,
                definitionId,
                status,
                currentNodeId,
                context,
                newHistory,
                startedAt,
                completedAt,
                errorMessage
        );
    }

    public ProcessInstance withUpdatedContext(ProcessContext newContext) {
        return new ProcessInstance(
                id,
                definitionId,
                status,
                currentNodeId,
                newContext,
                history,
                startedAt,
                completedAt,
                errorMessage
        );
    }

    public ProcessInstance failed(String errorMessage) {
        return new ProcessInstance(
                id,
                definitionId,
                ProcessStatus.FAILED,
                currentNodeId,
                context,
                history,
                startedAt,
                Instant.now(),
                errorMessage
        );
    }

    public boolean isFinished() {
        return status == ProcessStatus.COMPLETED || status == ProcessStatus.REJECTED || status == ProcessStatus.FAILED || status == ProcessStatus.TERMINATED;
    }
}
