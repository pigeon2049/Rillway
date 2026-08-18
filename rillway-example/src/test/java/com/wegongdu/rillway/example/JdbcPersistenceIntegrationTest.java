package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.example.workflow.PurchaseWorkflowFactory;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RillwayExampleApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class JdbcPersistenceIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessInstanceRepository instanceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ProcessDefinition purchaseDefinition = PurchaseWorkflowFactory.createPurchaseWorkflow();

    @Test
    @DisplayName("should automatically create tables and persist instance and task to database")
    void should_auto_create_tables_and_persist() {
        // 1. Verify tables exist in database
        Integer instanceTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rillway_instance", Integer.class
        );
        assertThat(instanceTableCount).isNotNull();

        // 2. Start small amount purchase which routes to manager-approval
        String businessKey = "PURCHASE_ORDER_DB_9901";
        ProcessContext context = ProcessContext.builder()
                .initiator("George")
                .variable("amount", new BigDecimal("2500"))
                .variable("item", "人体工学椅")
                .variable("hasInvoice", true)
                .build();

        ProcessInstance instance = processEngine.start(purchaseDefinition, businessKey, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.businessKey()).isEqualTo(businessKey);

        // 3. Verify instance persisted in database via repository
        ProcessInstance loadedFromDb = instanceRepository.findById(instance.id()).orElseThrow();
        assertThat(loadedFromDb.businessKey()).isEqualTo(businessKey);
        assertThat(loadedFromDb.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);

        // 4. Query pending task from database via TaskService
        List<Task> pendingTasks = taskService.findPendingTasks("user-mgr-01", List.of("DEPARTMENT_MANAGER"));
        assertThat(pendingTasks).anyMatch(t -> businessKey.equals(t.businessKey()) && t.status() == TaskStatus.PENDING);

        Task targetTask = pendingTasks.stream()
                .filter(t -> businessKey.equals(t.businessKey()))
                .findFirst()
                .orElseThrow();

        // 5. Complete task via TaskService
        ApproveDecision decision = ApproveDecision.of(
                Actor.HumanActor.of("user-mgr-01", "DEPARTMENT_MANAGER"),
                "数据库持久化测试同意审批"
        );
        ProcessInstance resumed = taskService.completeTask(targetTask.id(), decision);

        assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(resumed.currentNodeId()).isEqualTo("end");

        // 6. Verify database record is updated to COMPLETED
        ProcessInstance finalDbInstance = instanceRepository.findById(instance.id()).orElseThrow();
        assertThat(finalDbInstance.status()).isEqualTo(ProcessStatus.COMPLETED);

        Task finalDbTask = taskService.getTask(targetTask.id()).orElseThrow();
        assertThat(finalDbTask.status()).isEqualTo(TaskStatus.COMPLETED);
    }
}
