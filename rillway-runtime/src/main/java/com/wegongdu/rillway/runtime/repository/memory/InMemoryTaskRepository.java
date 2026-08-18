package com.wegongdu.rillway.runtime.repository.memory;

import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of TaskRepository.
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, Task> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        if (task != null && task.id() != null) {
            storage.put(task.id(), task);
        }
    }

    @Override
    public void update(Task task) {
        if (task != null && task.id() != null) {
            storage.put(task.id(), task);
        }
    }

    @Override
    public Optional<Task> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Task> findByProcessInstanceId(String processInstanceId) {
        if (processInstanceId == null) return List.of();
        return storage.values().stream()
                .filter(t -> processInstanceId.equals(t.processInstanceId()))
                .toList();
    }

    @Override
    public List<Task> findByBusinessKey(String businessKey) {
        if (businessKey == null) return List.of();
        return storage.values().stream()
                .filter(t -> businessKey.equals(t.businessKey()))
                .toList();
    }

    @Override
    public List<Task> findPendingTasksForUser(String userId, List<String> roles) {
        List<String> safeRoles = roles != null ? roles : Collections.emptyList();
        return storage.values().stream()
                .filter(t -> t.status() == TaskStatus.PENDING)
                .filter(t -> {
                    // Match assignee user
                    if (userId != null && userId.equals(t.assigneeUser())) {
                        return true;
                    }
                    // Match candidate users
                    if (userId != null && t.candidateUsers().contains(userId)) {
                        return true;
                    }
                    // Match assignee role
                    if (t.assigneeRole() != null && safeRoles.contains(t.assigneeRole())) {
                        return true;
                    }
                    // Match candidate roles
                    if (t.candidateRoles().stream().anyMatch(safeRoles::contains)) {
                        return true;
                    }
                    // If no assignee specified, visible as unclaimed task
                    return t.assigneeUser() == null && t.assigneeRole() == null
                            && t.candidateUsers().isEmpty() && t.candidateRoles().isEmpty();
                })
                .toList();
    }

    @Override
    public List<Task> findTasksByStatus(TaskStatus status) {
        if (status == null) return List.of();
        return storage.values().stream()
                .filter(t -> t.status() == status)
                .toList();
    }
}
