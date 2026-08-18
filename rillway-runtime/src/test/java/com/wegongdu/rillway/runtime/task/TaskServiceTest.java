package com.wegongdu.rillway.runtime.task;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.engine.StandardProcessEngine;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskServiceTest {

    private InMemoryProcessInstanceRepository instanceRepository;
    private InMemoryTaskRepository taskRepository;
    private InMemoryExecutionHistoryRepository historyRepository;
    private ProcessEngine processEngine;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        instanceRepository = new InMemoryProcessInstanceRepository();
        taskRepository = new InMemoryTaskRepository();
        historyRepository = new InMemoryExecutionHistoryRepository();

        processEngine = StandardProcessEngine.builder()
                .instanceRepository(instanceRepository)
                .taskRepository(taskRepository)
                .historyRepository(historyRepository)
                .build();

        taskService = new StandardTaskService(taskRepository, instanceRepository, processEngine);
    }

    @Test
    @DisplayName("should generate pending task and allow user to complete it via TaskService")
    void should_generate_task_and_complete() {
        ProcessDefinition definition = ProcessDefinition.builder("leave-proc")
                .startNode("start")
                .humanNode("manager-approval", h -> h
                        .assigneeRole("HR_MANAGER")
                        .assigneeUser("hr-alice")
                )
                .endNode("end")
                .edge("start", "manager-approval")
                .edge("manager-approval", "end")
                .build();

        // 1. Start process with business key
        ProcessInstance instance = processEngine.start(
                definition,
                "LEAVE_ORDER_2026_001",
                ProcessContext.builder().variable("days", 3).build()
        );

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.businessKey()).isEqualTo("LEAVE_ORDER_2026_001");

        // 2. Query pending tasks for HR_MANAGER / hr-alice
        List<Task> pendingTasks = taskService.findPendingTasks("hr-alice", List.of("HR_MANAGER"));
        assertThat(pendingTasks).hasSize(1);
        Task pendingTask = pendingTasks.get(0);
        assertThat(pendingTask.nodeId()).isEqualTo("manager-approval");
        assertThat(pendingTask.businessKey()).isEqualTo("LEAVE_ORDER_2026_001");
        assertThat(pendingTask.status()).isEqualTo(TaskStatus.PENDING);

        // 3. Complete task
        ProcessInstance completed = taskService.completeTask(
                pendingTask.id(),
                ApproveDecision.of(Actor.HumanActor.of("hr-alice", "HR_MANAGER"), "准假")
        );

        assertThat(completed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(completed.currentNodeId()).isEqualTo("end");

        // 4. Verify task status is updated to COMPLETED
        Task reloadedTask = taskService.getTask(pendingTask.id()).orElseThrow();
        assertThat(reloadedTask.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(reloadedTask.completedAt()).isNotNull();

        // 5. Query pending tasks again should be empty
        assertThat(taskService.findPendingTasks("hr-alice", List.of("HR_MANAGER"))).isEmpty();
    }
}
