package com.wegongdu.rillway.runtime.repository;

import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import java.util.List;
import java.util.Optional;

/**
 * Storage SPI for managing Human task records.
 */
public interface TaskRepository {

    void save(Task task);

    void update(Task task);

    Optional<Task> findById(String id);

    List<Task> findByProcessInstanceId(String processInstanceId);

    List<Task> findByBusinessKey(String businessKey);

    List<Task> findPendingTasksForUser(String userId, List<String> roles);

    List<Task> findTasksByStatus(TaskStatus status);
}
