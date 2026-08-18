package com.wegongdu.rillway.runtime.executor.impl;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.RouteDecision;
import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.node.RuleNode;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import java.util.List;

/**
 * Executor for RuleNode.
 */
public class RuleNodeExecutor implements NodeExecutor<RuleNode> {

    @Override
    public boolean supports(Node node) {
        return node instanceof RuleNode;
    }

    @Override
    public NodeExecutionResult execute(RuleNode node, ExecutionContext context) {
        ProcessContext procCtx = context.context();

        // 1. Evaluate branches in order
        for (RuleNode.RuleBranch branch : node.branches()) {
            try {
                if (branch.condition().test(procCtx)) {
                    RouteDecision decision = RouteDecision.of(
                            Actor.RuleActor.of(node.id(), node.name()),
                            branch.targetNodeId(),
                            "Matched condition: " + branch.description()
                    );
                    return NodeExecutionResult.advance(branch.targetNodeId(), decision);
                }
            } catch (Exception ex) {
                return NodeExecutionResult.failed("Rule evaluation failed on branch [" + branch.description() + "]: " + ex.getMessage());
            }
        }

        // 2. Fall back to defaultTargetNodeId
        if (node.defaultTargetNodeId() != null && !node.defaultTargetNodeId().isBlank()) {
            RouteDecision decision = RouteDecision.of(
                    Actor.RuleActor.of(node.id(), node.name()),
                    node.defaultTargetNodeId(),
                    "Default fallback branch taken"
            );
            return NodeExecutionResult.advance(node.defaultTargetNodeId(), decision);
        }

        // 3. Fall back to outgoing edge
        List<Edge> edges = context.definition().outgoingEdges(node.id());
        if (!edges.isEmpty()) {
            String target = edges.get(0).targetNodeId();
            RouteDecision decision = RouteDecision.of(
                    Actor.RuleActor.of(node.id(), node.name()),
                    target,
                    "Default edge taken"
            );
            return NodeExecutionResult.advance(target, decision);
        }

        return NodeExecutionResult.failed("No matching rule branch or default target found for node: " + node.id());
    }
}
