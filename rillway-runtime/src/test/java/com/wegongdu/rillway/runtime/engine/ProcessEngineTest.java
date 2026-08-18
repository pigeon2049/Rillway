package com.wegongdu.rillway.runtime.engine;

import com.wegongdu.rillway.agent.guard.AgentAuthorityGuard;
import com.wegongdu.rillway.agent.model.AgentDecision;
import com.wegongdu.rillway.agent.registry.InMemoryAgentRegistry;
import com.wegongdu.rillway.audit.event.AuditEvents;
import com.wegongdu.rillway.audit.sink.InMemoryAuditSink;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.policy.provider.InMemoryPolicyProvider;
import com.wegongdu.rillway.runtime.executor.impl.AgentNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.EndNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.HumanNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.RuleNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.StartNodeExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessEngineTest {

    private InMemoryAgentRegistry agentRegistry;
    private InMemoryPolicyProvider policyProvider;
    private InMemoryAuditSink auditSink;
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        agentRegistry = new InMemoryAgentRegistry();
        policyProvider = new InMemoryPolicyProvider();
        auditSink = new InMemoryAuditSink();

        engine = StandardProcessEngine.builder()
                .auditSink(auditSink)
                .addExecutors(List.of(
                        new StartNodeExecutor(),
                        new EndNodeExecutor(),
                        new HumanNodeExecutor(),
                        new RuleNodeExecutor(),
                        new AgentNodeExecutor(agentRegistry, policyProvider, new AgentAuthorityGuard(), auditSink)
                ))
                .build();
    }

    @Test
    @DisplayName("should route to manager when amount is below 5000")
    void should_route_to_manager_when_amount_is_below_5000() {
        ProcessDefinition definition = ProcessDefinition.builder("purchase-proc")
                .startNode("start")
                .ruleNode("amount-check", r -> r
                        .when("Below 5000", ctx -> ctx.getDecimal("amount").compareTo(new BigDecimal("5000")) < 0, "manager-approval")
                        .otherwise("procurement-approval")
                )
                .humanNode("manager-approval", h -> h.assigneeRole("MANAGER"))
                .humanNode("procurement-approval", h -> h.assigneeRole("PROCUREMENT"))
                .endNode("end")
                .edge("start", "amount-check")
                .edge("manager-approval", "end")
                .edge("procurement-approval", "end")
                .build();

        ProcessContext ctx = ProcessContext.builder()
                .variable("amount", new BigDecimal("3000"))
                .build();

        ProcessInstance instance = engine.start(definition, ctx);

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("manager-approval");

        // Resume with manager approval
        ApproveDecision managerDecision = ApproveDecision.of(Actor.HumanActor.of("user-101", "MANAGER"), "Approved by manager");
        ProcessInstance resumed = engine.resume(instance, managerDecision);

        assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(resumed.currentNodeId()).isEqualTo("end");
    }

    @Test
    @DisplayName("should allow agent to approve with delegated authority")
    void should_allow_agent_to_approve_with_delegated_authority() {
        agentRegistry.register(new com.wegongdu.rillway.agent.spi.Agent() {
            @Override
            public String id() {
                return "purchase-agent";
            }

            @Override
            public AgentDecision decide(com.wegongdu.rillway.agent.model.AgentContext context) {
                return AgentDecision.of(
                        ApproveDecision.of(Actor.AgentActor.of("purchase-agent"), "Auto-approved within policy bounds")
                );
            }
        });

        ProcessDefinition definition = ProcessDefinition.builder("agent-proc")
                .startNode("start")
                .agentNode("agent-node", a -> a
                        .agentId("purchase-agent")
                        .authority(AgentAuthority.DELEGATED)
                        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT)
                        .defaultTargetNodeId("end")
                )
                .endNode("end")
                .edge("start", "agent-node")
                .build();

        ProcessInstance instance = engine.start(definition, ProcessContext.empty());

        assertThat(instance.status()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(instance.currentNodeId()).isEqualTo("end");
        assertThat(auditSink.getEventsOfType(AuditEvents.AgentInvoked.class)).hasSize(1);
        assertThat(auditSink.getEventsOfType(AuditEvents.AgentDecisionMade.class)).hasSize(1);
    }

    @Test
    @DisplayName("should fallback to human when agent cannot decide")
    void should_fallback_to_human_when_agent_cannot_decide() {
        agentRegistry.register(new com.wegongdu.rillway.agent.spi.Agent() {
            @Override
            public String id() {
                return "uncertain-agent";
            }

            @Override
            public AgentDecision decide(com.wegongdu.rillway.agent.model.AgentContext context) {
                return AgentDecision.fallback("Missing required supplier compliance report");
            }
        });

        ProcessDefinition definition = ProcessDefinition.builder("fallback-proc")
                .startNode("start")
                .agentNode("agent-node", a -> a
                        .agentId("uncertain-agent")
                        .authority(AgentAuthority.DELEGATED)
                        .allowedDecisions(DecisionType.APPROVE)
                        .fallbackNodeId("human-fallback")
                        .defaultTargetNodeId("end")
                )
                .humanNode("human-fallback", h -> h.assigneeRole("COMPLIANCE_OFFICER"))
                .endNode("end")
                .edge("start", "agent-node")
                .edge("human-fallback", "end")
                .build();

        ProcessInstance instance = engine.start(definition, ProcessContext.empty());

        // Agent should trigger fallback and suspend at human-fallback
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("human-fallback");
    }

    @Test
    @DisplayName("should fallback to human when advisory agent attempts illegal direct approve")
    void should_fallback_when_advisory_agent_violates_authority() {
        agentRegistry.register(new com.wegongdu.rillway.agent.spi.Agent() {
            @Override
            public String id() {
                return "advisory-agent";
            }

            @Override
            public AgentDecision decide(com.wegongdu.rillway.agent.model.AgentContext context) {
                return AgentDecision.of(ApproveDecision.of(Actor.AgentActor.of("advisory-agent"), "Trying to approve independently"));
            }
        });

        ProcessDefinition definition = ProcessDefinition.builder("advisory-proc")
                .startNode("start")
                .agentNode("agent-node", a -> a
                        .agentId("advisory-agent")
                        .authority(AgentAuthority.ADVISORY) // ADVISORY cannot issue ApproveDecision directly
                        .allowedDecisions(DecisionType.APPROVE)
                        .fallbackNodeId("human-review")
                        .defaultTargetNodeId("end")
                )
                .humanNode("human-review", h -> h.assigneeRole("DIRECTOR"))
                .endNode("end")
                .edge("start", "agent-node")
                .edge("human-review", "end")
                .build();

        ProcessInstance instance = engine.start(definition, ProcessContext.empty());

        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.currentNodeId()).isEqualTo("human-review");
    }
}
