package com.wegongdu.rillway.runtime.executor.impl;

import com.wegongdu.rillway.agent.guard.AgentAuthorityGuard;
import com.wegongdu.rillway.agent.model.AgentContext;
import com.wegongdu.rillway.agent.model.AgentDecision;
import com.wegongdu.rillway.agent.spi.Agent;
import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.audit.event.AuditEvents;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.decision.RouteDecision;
import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.node.AgentNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.policy.model.PolicyDocument;
import com.wegongdu.rillway.policy.model.PolicyQuery;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Executor for AgentNode.
 */
public class AgentNodeExecutor implements NodeExecutor<AgentNode> {

    private final AgentRegistry agentRegistry;
    private final PolicyProvider policyProvider;
    private final AgentAuthorityGuard authorityGuard;
    private final AuditSink auditSink;

    public AgentNodeExecutor(
            AgentRegistry agentRegistry,
            PolicyProvider policyProvider,
            AgentAuthorityGuard authorityGuard,
            AuditSink auditSink
    ) {
        this.agentRegistry = agentRegistry;
        this.policyProvider = policyProvider;
        this.authorityGuard = authorityGuard != null ? authorityGuard : new AgentAuthorityGuard();
        this.auditSink = auditSink;
    }

    @Override
    public boolean supports(Node node) {
        return node instanceof AgentNode;
    }

    @Override
    public NodeExecutionResult execute(AgentNode node, ExecutionContext context) {
        String agentId = node.agentId();

        // 1. Audit Agent Invocation
        if (auditSink != null) {
            auditSink.publish(new AuditEvents.AgentInvoked(
                    null,
                    context.instance().id(),
                    context.definition().id(),
                    node.id(),
                    agentId,
                    node.authority(),
                    null
            ));
        }

        // 2. Discover Agent from Registry
        Optional<Agent> agentOpt = agentRegistry.find(agentId);
        if (agentOpt.isEmpty()) {
            if (node.fallbackNodeId() != null) {
                return executeFallback(node, "Agent not found in registry: " + agentId);
            }
            return NodeExecutionResult.failed("Agent not registered: " + agentId);
        }

        // 3. Load policies for the agent context
        List<PolicyDocument> availablePolicies = new ArrayList<>();
        if (policyProvider != null && node.policies() != null) {
            for (String policyId : node.policies()) {
                policyProvider.getPolicyDocument(policyId).ifPresent(availablePolicies::add);
                policyProvider.findPolicies(PolicyQuery.of(policyId, List.of(policyId))).forEach(availablePolicies::add);
            }
        }

        // 4. Construct Agent Context & Invoke
        Agent agent = agentOpt.get();
        AgentContext agentContext = AgentContext.of(context.instance(), node, availablePolicies);

        AgentDecision agentDecision;
        try {
            agentDecision = agent.decide(agentContext);
        } catch (Exception ex) {
            if (node.fallbackNodeId() != null) {
                return executeFallback(node, "Agent execution error: " + ex.getMessage());
            }
            return NodeExecutionResult.failed("Agent execution failed: " + ex.getMessage());
        }

        // 5. Handle Fallback request
        if (agentDecision.needsFallback()) {
            if (node.fallbackNodeId() != null) {
                return executeFallback(node, agentDecision.fallbackReason() != null ? agentDecision.fallbackReason() : "Agent requested fallback");
            }
            return NodeExecutionResult.failed("Agent requested fallback, but no fallbackNodeId configured on node: " + node.id());
        }

        Decision decision = agentDecision.decision();
        if (decision == null) {
            if (node.fallbackNodeId() != null) {
                return executeFallback(node, "Agent returned null decision");
            }
            return NodeExecutionResult.failed("Agent returned null decision without fallback");
        }

        // 6. Authority Guard Validation
        try {
            authorityGuard.validateDecision(node, decision);
        } catch (Exception ex) {
            if (node.fallbackNodeId() != null) {
                return executeFallback(node, "Authority guard violation fallback: " + ex.getMessage());
            }
            throw ex;
        }

        // 7. Audit Decision Made
        if (auditSink != null) {
            auditSink.publish(new AuditEvents.AgentDecisionMade(
                    null,
                    context.instance().id(),
                    context.definition().id(),
                    node.id(),
                    agentId,
                    node.authority(),
                    decision,
                    null
            ));
        }

        // 8. Resolve Next Target
        return resolveNextNode(node, decision, context);
    }

    private NodeExecutionResult executeFallback(AgentNode node, String reason) {
        RouteDecision fallbackDecision = RouteDecision.of(
                Actor.AgentActor.of(node.agentId()),
                node.fallbackNodeId(),
                "Agent fallback triggered: " + reason
        );
        return NodeExecutionResult.advance(node.fallbackNodeId(), fallbackDecision);
    }

    private NodeExecutionResult resolveNextNode(AgentNode node, Decision decision, ExecutionContext context) {
        DecisionType decisionType = decision.type();

        // 1. If RouteDecision has explicit target
        if (decision instanceof RouteDecision rd) {
            return NodeExecutionResult.advance(rd.targetNodeId(), decision);
        }

        // 2. Check node.decisionRoutes()
        if (node.decisionRoutes().containsKey(decisionType)) {
            return NodeExecutionResult.advance(node.decisionRoutes().get(decisionType), decision);
        }

        // 3. Check outgoing edges matching onDecision
        for (Edge edge : context.definition().outgoingEdges(node.id())) {
            if (edge.onDecision() != null && edge.onDecision() == decisionType) {
                return NodeExecutionResult.advance(edge.targetNodeId(), decision);
            }
        }

        // 4. Default target node on node
        if (node.defaultTargetNodeId() != null) {
            return NodeExecutionResult.advance(node.defaultTargetNodeId(), decision);
        }

        // 5. Default edge
        List<Edge> edges = context.definition().outgoingEdges(node.id());
        for (Edge edge : edges) {
            if (edge.onDecision() == null) {
                return NodeExecutionResult.advance(edge.targetNodeId(), decision);
            }
        }

        return NodeExecutionResult.failed("No matching next path found for Agent node [" + node.id() + "] with decision " + decisionType);
    }
}
