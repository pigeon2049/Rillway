package com.wegongdu.rillway.runtime.executor.impl;

import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.node.HumanNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import java.util.List;

/**
 * Executor for HumanNode.
 */
public class HumanNodeExecutor implements NodeExecutor<HumanNode> {

    @Override
    public boolean supports(Node node) {
        return node instanceof HumanNode;
    }

    @Override
    public NodeExecutionResult execute(HumanNode node, ExecutionContext context) {
        Decision decision = context.inputDecision();

        // If no decision has been made yet, suspend the workflow waiting for human interaction
        if (decision == null) {
            return NodeExecutionResult.suspend();
        }

        // Advance based on human decision and outgoing edges
        List<Edge> edges = context.definition().outgoingEdges(node.id());

        // 1. Try to match edge with specific onDecision type
        for (Edge edge : edges) {
            if (edge.onDecision() != null && edge.onDecision() == decision.type()) {
                return NodeExecutionResult.advance(edge.targetNodeId(), decision);
            }
        }

        // 2. Try default outgoing edge
        for (Edge edge : edges) {
            if (edge.onDecision() == null) {
                return NodeExecutionResult.advance(edge.targetNodeId(), decision);
            }
        }

        // If decision is reject and no specific edge, complete as rejected or find end
        if (decision.type() == DecisionType.REJECT) {
            return NodeExecutionResult.complete(decision);
        }

        return NodeExecutionResult.failed("No matching outgoing path found for human node [" + node.id() + "] with decision " + decision.type());
    }
}
