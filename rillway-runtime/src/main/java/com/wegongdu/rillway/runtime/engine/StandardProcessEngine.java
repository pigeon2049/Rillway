package com.wegongdu.rillway.runtime.engine;

import com.wegongdu.rillway.audit.event.AuditEvents;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.audit.sink.NoOpAuditSink;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ExecutionRecord;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.node.EndNode;
import com.wegongdu.rillway.core.node.HumanNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.validation.ProcessValidator;
import com.wegongdu.rillway.core.validation.StandardProcessValidator;
import com.wegongdu.rillway.core.validation.ValidationResult;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.EndNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.HumanNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.RuleNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.StartNodeExecutor;
import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.repository.ExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard robust implementation of ProcessEngine with persistence and Task integration.
 */
public class StandardProcessEngine implements ProcessEngine {

    private final List<NodeExecutor<? extends Node>> executors;
    private final ProcessValidator validator;
    private final AuditSink auditSink;
    private final ProcessInstanceRepository instanceRepository;
    private final TaskRepository taskRepository;
    private final ExecutionHistoryRepository historyRepository;
    private final HumanAssigneeResolver assigneeResolver;
    private final Map<String, ProcessDefinition> definitionCache = new ConcurrentHashMap<>();

    public StandardProcessEngine(
            List<NodeExecutor<? extends Node>> executors,
            ProcessValidator validator,
            AuditSink auditSink,
            ProcessInstanceRepository instanceRepository,
            TaskRepository taskRepository,
            ExecutionHistoryRepository historyRepository
    ) {
        this(executors, validator, auditSink, instanceRepository, taskRepository, historyRepository, (node, ctx) ->
                HumanAssigneeResolver.ResolvedAssignee.of(node.assigneeUser(), node.assigneeRole(), node.candidateUsers(), node.candidateRoles()));
    }

    public StandardProcessEngine(
            List<NodeExecutor<? extends Node>> executors,
            ProcessValidator validator,
            AuditSink auditSink,
            ProcessInstanceRepository instanceRepository,
            TaskRepository taskRepository,
            ExecutionHistoryRepository historyRepository,
            HumanAssigneeResolver assigneeResolver
    ) {
        this.executors = executors != null ? List.copyOf(executors) : List.of();
        this.validator = validator != null ? validator : new StandardProcessValidator();
        this.auditSink = auditSink != null ? auditSink : NoOpAuditSink.INSTANCE;
        this.instanceRepository = instanceRepository != null ? instanceRepository : new InMemoryProcessInstanceRepository();
        this.taskRepository = taskRepository != null ? taskRepository : new InMemoryTaskRepository();
        this.historyRepository = historyRepository != null ? historyRepository : new InMemoryExecutionHistoryRepository();
        this.assigneeResolver = assigneeResolver != null ? assigneeResolver : (node, ctx) ->
                HumanAssigneeResolver.ResolvedAssignee.of(node.assigneeUser(), node.assigneeRole(), node.candidateUsers(), node.candidateRoles());
    }

    public static Builder builder() {
        return new Builder();
    }

    public void registerDefinition(ProcessDefinition definition) {
        if (definition != null) {
            definitionCache.put(definition.id(), definition);
        }
    }

    @Override
    public ProcessInstance start(ProcessDefinition definition, String businessKey, ProcessContext context) {
        Objects.requireNonNull(definition, "ProcessDefinition must not be null");
        if (context == null) {
            context = ProcessContext.empty();
        }

        // 1. Validate Definition
        ValidationResult validation = validator.validate(definition);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("ProcessDefinition is invalid: " + validation.errors());
        }

        registerDefinition(definition);

        // 2. Initialize Instance & Persist
        String startNodeId = definition.getStartNode().id();
        ProcessInstance instance = ProcessInstance.create(definition.id(), businessKey, startNodeId, context);
        instanceRepository.save(instance);

        // 3. Publish Start Event
        auditSink.publish(new AuditEvents.ProcessStarted(
                null,
                instance.id(),
                definition.id(),
                startNodeId,
                context.initiator(),
                context,
                Instant.now()
        ));

        // 4. Run Execution Loop
        return driveWorkflow(definition, instance, null);
    }

    @Override
    public ProcessInstance resume(ProcessInstance instance, Decision decision) {
        Objects.requireNonNull(instance, "ProcessInstance must not be null");
        Objects.requireNonNull(decision, "Decision must not be null");

        if (instance.isFinished()) {
            throw new IllegalStateException("Cannot resume finished process instance: " + instance.id());
        }

        ProcessDefinition definition = definitionCache.get(instance.definitionId());
        if (definition == null) {
            throw new IllegalStateException("ProcessDefinition not found in engine cache: " + instance.definitionId());
        }

        return driveWorkflow(definition, instance, decision);
    }

    private ProcessInstance driveWorkflow(
            ProcessDefinition definition,
            ProcessInstance instance,
            Decision resumeDecision
    ) {
        ProcessInstance currentInstance = instance;
        Decision pendingDecision = resumeDecision;

        while (!currentInstance.isFinished()) {
            String currentNodeId = currentInstance.currentNodeId();
            Node currentNode = definition.nodes().get(currentNodeId);

            if (currentNode == null) {
                String errMsg = "Node not found in definition: " + currentNodeId;
                currentInstance = currentInstance.failed(errMsg);
                instanceRepository.update(currentInstance);
                auditSink.publish(new AuditEvents.ProcessFailed(null, currentInstance.id(), definition.id(), currentNodeId, errMsg, null));
                break;
            }

            Instant enteredAt = Instant.now();
            auditSink.publish(new AuditEvents.NodeEntered(
                    null,
                    currentInstance.id(),
                    definition.id(),
                    currentNode.id(),
                    currentNode.name(),
                    currentNode.type(),
                    enteredAt
            ));

            NodeExecutor<Node> executor = findExecutor(currentNode);
            if (executor == null) {
                String errMsg = "No executor found for node type: " + currentNode.type();
                currentInstance = currentInstance.failed(errMsg);
                instanceRepository.update(currentInstance);
                auditSink.publish(new AuditEvents.ProcessFailed(null, currentInstance.id(), definition.id(), currentNode.id(), errMsg, null));
                break;
            }

            ExecutionContext execCtx = ExecutionContext.of(definition, currentInstance, pendingDecision);
            pendingDecision = null; // consume decision

            NodeExecutionResult result;
            try {
                result = executor.execute(currentNode, execCtx);
            } catch (Exception ex) {
                String errMsg = "Execution failed on node [" + currentNode.id() + "]: " + ex.getMessage();
                ExecutionRecord failedRecord = ExecutionRecord.of(currentNode.id(), currentNode.name(), currentNode.type(), enteredAt).failed(errMsg);
                currentInstance = currentInstance.withHistoryRecord(failedRecord).failed(errMsg);
                historyRepository.save(currentInstance.id(), failedRecord);
                instanceRepository.update(currentInstance);
                auditSink.publish(new AuditEvents.ProcessFailed(null, currentInstance.id(), definition.id(), currentNode.id(), errMsg, null));
                break;
            }

            if (result.status() == NodeExecutionResult.Status.SUSPEND) {
                currentInstance = currentInstance.withStatusAndNode(ProcessStatus.WAITING_FOR_DECISION, currentNode.id());
                instanceRepository.update(currentInstance);

                // If HumanNode suspended, generate pending Task record if not already created
                if (currentNode instanceof HumanNode hn) {
                    List<Task> existingPending = taskRepository.findByProcessInstanceId(currentInstance.id()).stream()
                            .filter(t -> t.nodeId().equals(hn.id()) && t.status() == com.wegongdu.rillway.core.model.TaskStatus.PENDING)
                            .toList();
                    if (existingPending.isEmpty()) {
                        HumanAssigneeResolver.ResolvedAssignee resolved = assigneeResolver.resolve(hn, currentInstance.context());
                        Task task = Task.createPending(
                                currentInstance.id(),
                                currentInstance.businessKey(),
                                definition.id(),
                                hn.id(),
                                hn.name(),
                                resolved.assigneeUser(),
                                resolved.assigneeRole(),
                                resolved.candidateUsers(),
                                resolved.candidateRoles()
                        );
                        taskRepository.save(task);
                    }
                }
                break;
            } else if (result.status() == NodeExecutionResult.Status.ADVANCE) {
                Actor actor = result.decision() != null ? result.decision().actor() : Actor.RuleActor.of("engine");
                ExecutionRecord completedRecord = ExecutionRecord.of(currentNode.id(), currentNode.name(), currentNode.type(), enteredAt).completed(actor, result.decision());
                currentInstance = currentInstance.withHistoryRecord(completedRecord);
                historyRepository.save(currentInstance.id(), completedRecord);

                auditSink.publish(new AuditEvents.NodeCompleted(
                        null,
                        currentInstance.id(),
                        definition.id(),
                        currentNode.id(),
                        currentNode.name(),
                        currentNode.type(),
                        actor,
                        result.decision(),
                        Instant.now()
                ));

                if (result.decision() != null) {
                    auditSink.publish(new AuditEvents.DecisionMade(
                            null,
                            currentInstance.id(),
                            definition.id(),
                            currentNode.id(),
                            actor,
                            result.decision(),
                            Instant.now()
                    ));
                }

                currentInstance = currentInstance.withStatusAndNode(ProcessStatus.RUNNING, result.nextNodeId());
                instanceRepository.update(currentInstance);
            } else if (result.status() == NodeExecutionResult.Status.COMPLETE) {
                Actor actor = result.decision() != null ? result.decision().actor() : Actor.RuleActor.of("engine");
                ExecutionRecord completedRecord = ExecutionRecord.of(currentNode.id(), currentNode.name(), currentNode.type(), enteredAt).completed(actor, result.decision());
                currentInstance = currentInstance.withHistoryRecord(completedRecord);
                historyRepository.save(currentInstance.id(), completedRecord);

                boolean hasRejection = (result.decision() != null && result.decision().type() == DecisionType.REJECT)
                        || currentInstance.history().stream().anyMatch(h -> h.decision() != null && h.decision().type() == DecisionType.REJECT);
                boolean isSuccess = !hasRejection && (!(currentNode instanceof EndNode en) || en.isSuccess());
                ProcessStatus finalStatus = hasRejection ? ProcessStatus.REJECTED : ProcessStatus.COMPLETED;

                currentInstance = currentInstance.withStatusAndNode(finalStatus, currentNode.id());
                instanceRepository.update(currentInstance);

                auditSink.publish(new AuditEvents.ProcessCompleted(
                        null,
                        currentInstance.id(),
                        definition.id(),
                        currentNode.id(),
                        isSuccess,
                        Instant.now()
                ));
                break;
            } else if (result.status() == NodeExecutionResult.Status.FAILED) {
                String errMsg = result.errorMessage() != null ? result.errorMessage() : "Node execution reported failure";
                ExecutionRecord failedRecord = ExecutionRecord.of(currentNode.id(), currentNode.name(), currentNode.type(), enteredAt).failed(errMsg);
                currentInstance = currentInstance.withHistoryRecord(failedRecord).failed(errMsg);
                historyRepository.save(currentInstance.id(), failedRecord);
                instanceRepository.update(currentInstance);
                auditSink.publish(new AuditEvents.ProcessFailed(null, currentInstance.id(), definition.id(), currentNode.id(), errMsg, null));
                break;
            }
        }

        return currentInstance;
    }

    @SuppressWarnings("unchecked")
    private NodeExecutor<Node> findExecutor(Node node) {
        for (NodeExecutor<? extends Node> executor : executors) {
            if (executor.supports(node)) {
                return (NodeExecutor<Node>) executor;
            }
        }
        return null;
    }

    public static class Builder {
        private final List<NodeExecutor<? extends Node>> executors = new ArrayList<>();
        private ProcessValidator validator;
        private AuditSink auditSink;
        private ProcessInstanceRepository instanceRepository;
        private TaskRepository taskRepository;
        private ExecutionHistoryRepository historyRepository;
        private HumanAssigneeResolver assigneeResolver;

        public Builder addExecutor(NodeExecutor<? extends Node> executor) {
            if (executor != null) {
                this.executors.add(executor);
            }
            return this;
        }

        public Builder addExecutors(List<NodeExecutor<? extends Node>> executors) {
            if (executors != null) {
                this.executors.addAll(executors);
            }
            return this;
        }

        public Builder validator(ProcessValidator validator) {
            this.validator = validator;
            return this;
        }

        public Builder auditSink(AuditSink auditSink) {
            this.auditSink = auditSink;
            return this;
        }

        public Builder instanceRepository(ProcessInstanceRepository instanceRepository) {
            this.instanceRepository = instanceRepository;
            return this;
        }

        public Builder taskRepository(TaskRepository taskRepository) {
            this.taskRepository = taskRepository;
            return this;
        }

        public Builder historyRepository(ExecutionHistoryRepository historyRepository) {
            this.historyRepository = historyRepository;
            return this;
        }

        public Builder assigneeResolver(HumanAssigneeResolver assigneeResolver) {
            this.assigneeResolver = assigneeResolver;
            return this;
        }

        public StandardProcessEngine build() {
            if (this.executors.isEmpty()) {
                this.executors.add(new StartNodeExecutor());
                this.executors.add(new EndNodeExecutor());
                this.executors.add(new HumanNodeExecutor());
                this.executors.add(new RuleNodeExecutor());
            }
            return new StandardProcessEngine(
                    executors,
                    validator,
                    auditSink,
                    instanceRepository,
                    taskRepository,
                    historyRepository,
                    assigneeResolver
            );
        }
    }
}
