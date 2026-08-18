package com.wegongdu.rillway.runtime.executor.impl;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.node.EndNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.runtime.executor.ExecutionContext;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;

/**
 * Executor for EndNode.
 */
public class EndNodeExecutor implements NodeExecutor<EndNode> {

    @Override
    public boolean supports(Node node) {
        return node instanceof EndNode;
    }

    @Override
    public NodeExecutionResult execute(EndNode node, ExecutionContext context) {
        ApproveDecision decision = ApproveDecision.of(
                Actor.RuleActor.of("end-terminator"),
                "Workflow completed at end node: " + node.name()
        );
        return NodeExecutionResult.complete(decision);
    }
}
