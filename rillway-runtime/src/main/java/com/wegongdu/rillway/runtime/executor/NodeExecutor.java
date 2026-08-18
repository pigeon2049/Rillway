package com.wegongdu.rillway.runtime.executor;

import com.wegongdu.rillway.core.instance.NodeExecutionResult;
import com.wegongdu.rillway.core.node.Node;

/**
 * Strategy interface for executing nodes of specific types.
 *
 * @param <N> Node subclass type
 */
public interface NodeExecutor<N extends Node> {

    boolean supports(Node node);

    NodeExecutionResult execute(N node, ExecutionContext context);
}
