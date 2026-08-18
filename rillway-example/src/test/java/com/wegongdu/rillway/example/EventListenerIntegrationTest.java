package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.event.ProcessEvent;
import com.wegongdu.rillway.core.event.ProcessEventListener;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseApplication.class)
@Import(EventListenerIntegrationTest.TestListenerConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_event_listener_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class EventListenerIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private SpringAnnotationEventListener annotationListener;

    @Autowired
    private CustomInterfaceEventListener interfaceListener;

    @TestConfiguration
    static class TestListenerConfiguration {
        @Bean
        public SpringAnnotationEventListener springAnnotationEventListener() {
            return new SpringAnnotationEventListener();
        }

        @Bean
        public CustomInterfaceEventListener customInterfaceEventListener() {
            return new CustomInterfaceEventListener();
        }
    }

    @Component
    public static class SpringAnnotationEventListener {
        final List<ProcessEvent.ProcessStartedEvent> startedEvents = new CopyOnWriteArrayList<>();
        final List<ProcessEvent.NodeEnteredEvent> nodeEnteredEvents = new CopyOnWriteArrayList<>();
        final List<ProcessEvent.NodeCompletedEvent> nodeCompletedEvents = new CopyOnWriteArrayList<>();
        final List<ProcessEvent.ProcessCompletedEvent> completedEvents = new CopyOnWriteArrayList<>();
        final List<String> simulatedNotifications = new CopyOnWriteArrayList<>();

        public void clear() {
            startedEvents.clear();
            nodeEnteredEvents.clear();
            nodeCompletedEvents.clear();
            completedEvents.clear();
            simulatedNotifications.clear();
        }

        @EventListener
        public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
            startedEvents.add(event);
            simulatedNotifications.add("NOTIFY_START: Process [" + event.processInstanceId() + "] started for businessKey [" + event.businessKey() + "]");
        }

        @EventListener
        public void onNodeEntered(ProcessEvent.NodeEnteredEvent event) {
            nodeEnteredEvents.add(event);
            if (event.assigneeRole() != null) {
                simulatedNotifications.add("NOTIFY_TASK: Node [" + event.nodeId() + "] entered, notify role [" + event.assigneeRole() + "]");
            }
        }

        @EventListener
        public void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
            nodeCompletedEvents.add(event);
        }

        @EventListener
        public void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
            completedEvents.add(event);
            simulatedNotifications.add("NOTIFY_COMPLETE: Process [" + event.processInstanceId() + "] completed with success=" + event.isSuccess());
        }
    }

    @Component
    public static class CustomInterfaceEventListener implements ProcessEventListener {
        final List<ProcessEvent> allEvents = new CopyOnWriteArrayList<>();

        public void clear() {
            allEvents.clear();
        }

        @Override
        public void onEvent(ProcessEvent event) {
            allEvents.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        annotationListener.clear();
        interfaceListener.clear();
    }

    private ProcessDefinition buildLeaveProcess() {
        return ProcessDefinition.builder("leave-request-process")
                .name("员工请假审批流程")
                .startNode("start", "提交请假")
                .humanNode("manager-approval", human -> human
                        .name("部门经理审批")
                        .assigneeRole("DEPARTMENT_MANAGER")
                )
                .endNode("end", "流程结束")
                .edge("start", "manager-approval")
                .edge("manager-approval", "end")
                .build();
    }

    @Test
    @DisplayName("should trigger @EventListener and ProcessEventListener on start, node entered and approval completion")
    void should_trigger_listeners_on_full_approval_lifecycle() {
        ProcessDefinition definition = buildLeaveProcess();
        String businessKey = "leave_order:LEAVE_20260818_001";

        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("days", 3)
                .variable("reason", "Personal travel")
                .build();

        // 1. Start Process
        ProcessInstance instance = processEngine.start(definition, businessKey, context);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("manager-approval");

        // Verify ProcessStartedEvent
        assertThat(annotationListener.startedEvents).hasSize(1);
        ProcessEvent.ProcessStartedEvent startedEvent = annotationListener.startedEvents.get(0);
        assertThat(startedEvent.businessKey()).isEqualTo(businessKey);
        assertThat(startedEvent.initiator()).isEqualTo("Alice");
        assertThat(startedEvent.definitionId()).isEqualTo("leave-request-process");

        // Verify NodeEnteredEvent
        assertThat(annotationListener.nodeEnteredEvents).anyMatch(e ->
                "manager-approval".equals(e.nodeId()) && "DEPARTMENT_MANAGER".equals(e.assigneeRole())
        );

        // Verify simulated notifications
        assertThat(annotationListener.simulatedNotifications).contains(
                "NOTIFY_START: Process [" + instance.id() + "] started for businessKey [" + businessKey + "]",
                "NOTIFY_TASK: Node [manager-approval] entered, notify role [DEPARTMENT_MANAGER]"
        );

        // 2. Complete Approval Task
        List<Task> tasks = taskService.findTasksByProcessInstanceId(instance.id());
        assertThat(tasks).isNotEmpty();
        Task targetTask = tasks.get(0);

        ProcessInstance finishedInstance = taskService.completeTask(
                targetTask.id(),
                ApproveDecision.of(Actor.HumanActor.of("Bob", "DEPARTMENT_MANAGER"), "同意请假")
        );

        assertThat(finishedInstance.status()).isEqualTo(ProcessStatus.COMPLETED);

        // Verify NodeCompletedEvent & ProcessCompletedEvent
        assertThat(annotationListener.nodeCompletedEvents).anyMatch(e -> "manager-approval".equals(e.nodeId()));
        assertThat(annotationListener.completedEvents).hasSize(1);
        ProcessEvent.ProcessCompletedEvent completedEvent = annotationListener.completedEvents.get(0);
        assertThat(completedEvent.businessKey()).isEqualTo(businessKey);
        assertThat(completedEvent.isSuccess()).isTrue();
        assertThat(completedEvent.finalStatus()).isEqualTo(ProcessStatus.COMPLETED);

        // Verify simulated completion notification
        assertThat(annotationListener.simulatedNotifications).contains(
                "NOTIFY_COMPLETE: Process [" + instance.id() + "] completed with success=true"
        );

        // Verify interface listener also captured all events
        assertThat(interfaceListener.allEvents).isNotEmpty();
        assertThat(interfaceListener.allEvents).anyMatch(e -> e instanceof ProcessEvent.ProcessStartedEvent);
        assertThat(interfaceListener.allEvents).anyMatch(e -> e instanceof ProcessEvent.ProcessCompletedEvent);
    }

    @Test
    @DisplayName("should trigger @EventListener with rejected status when decision is rejected")
    void should_trigger_listener_on_rejection() {
        ProcessDefinition definition = buildLeaveProcess();
        String businessKey = "leave_order:LEAVE_20260818_002";

        ProcessContext context = ProcessContext.builder()
                .initiator("Alice")
                .variable("days", 30)
                .build();

        ProcessInstance instance = processEngine.start(definition, businessKey, context);

        List<Task> tasks = taskService.findTasksByProcessInstanceId(instance.id());
        assertThat(tasks).isNotEmpty();
        Task targetTask = tasks.get(0);

        ProcessInstance rejectedInstance = taskService.completeTask(
                targetTask.id(),
                RejectDecision.of(Actor.HumanActor.of("Bob", "DEPARTMENT_MANAGER"), "天数过长，不予批准")
        );

        assertThat(rejectedInstance.status()).isEqualTo(ProcessStatus.REJECTED);

        assertThat(annotationListener.completedEvents).hasSize(1);
        ProcessEvent.ProcessCompletedEvent completedEvent = annotationListener.completedEvents.get(0);
        assertThat(completedEvent.businessKey()).isEqualTo(businessKey);
        assertThat(completedEvent.isSuccess()).isFalse();
        assertThat(completedEvent.finalStatus()).isEqualTo(ProcessStatus.REJECTED);
    }
}
