package com.wegongdu.rillway.runtime.executor.impl;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.RouteDecision;
import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.node.StartNode;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import java.util.List;

/**
 * Executor for StartNode.
 */
public class StartNodeExecutor implements NodeExecutor<StartNode> {

    @Override
    public boolean supports(Node node) {
        return node instanceof StartNode;
    }

    @Override
    public NodeExecutionResult execute(StartNode node, ExecutionContext context) {
        List<Edge> edges = context.definition().outgoingEdges(node.id());
        if (edges.isEmpty()) {
            return NodeExecutionResult.failed("StartNode has no outgoing edge: " + node.id());
        }

        String nextNodeId = edges.get(0).targetNodeId();
        RouteDecision decision = RouteDecision.of(
                Actor.RuleActor.of("start-router"),
                nextNodeId,
                "Workflow initiated"
        );
        return NodeExecutionResult.advance(nextNodeId, decision);
    }
}
