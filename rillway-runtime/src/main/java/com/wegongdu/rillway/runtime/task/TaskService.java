package com.wegongdu.rillway.runtime.task;

import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.Task;
import java.util.List;
import java.util.Optional;

/**
 * High-level business facade for querying and completing human tasks.
 */
public interface TaskService {

    Optional<Task> getTask(String taskId);

    List<Task> findPendingTasks(String userId, List<String> roles);

    List<Task> findTasksByBusinessKey(String businessKey);

    List<Task> findTasksByProcessInstanceId(String processInstanceId);

    ProcessInstance completeTask(String taskId, Decision decision);

    /**
     * Transfers a pending task to another user (e.g. employee job reassignment / offboarding).
     */
    Task transferTask(String taskId, String newAssignee, String reason);

    /**
     * Transfers a pending task to another user or role.
     */
    Task transferTask(String taskId, String newAssignee, String newAssigneeRole, String reason);

    /**
     * Checks whether this task belongs to the final approval node before process completion.
     */
    boolean isTerminalTask(String taskId);
}
