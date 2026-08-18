package com.wegongdu.rillway.runtime.task;

import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Standard implementation of TaskService.
 */
public class StandardTaskService implements TaskService {

    private final TaskRepository taskRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final ProcessEngine processEngine;

    public StandardTaskService(
            TaskRepository taskRepository,
            ProcessInstanceRepository instanceRepository,
            ProcessEngine processEngine
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository must not be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine must not be null");
    }

    @Override
    public Optional<Task> getTask(String taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    public List<Task> findPendingTasks(String userId, List<String> roles) {
        return taskRepository.findPendingTasksForUser(userId, roles);
    }

    @Override
    public List<Task> findTasksByBusinessKey(String businessKey) {
        return taskRepository.findByBusinessKey(businessKey);
    }

    @Override
    public List<Task> findTasksByProcessInstanceId(String processInstanceId) {
        return taskRepository.findByProcessInstanceId(processInstanceId);
    }

    @Override
    public ProcessInstance completeTask(String taskId, Decision decision) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.status() != TaskStatus.PENDING) {
            throw new IllegalStateException("Task is not in PENDING status: " + taskId + " (current: " + task.status() + ")");
        }

        ProcessInstance instance = instanceRepository.findById(task.processInstanceId())
                .orElseThrow(() -> new IllegalStateException("ProcessInstance not found for task: " + task.processInstanceId()));

        // 1. Mark task as completed
        Task completedTask = task.complete();
        taskRepository.update(completedTask);

        // 2. Resume process workflow with the decision
        return processEngine.resume(instance, decision);
    }
}
