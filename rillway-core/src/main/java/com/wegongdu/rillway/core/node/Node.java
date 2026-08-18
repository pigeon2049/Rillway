package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.model.NodeType;

/**
 * Sealed interface representing a workflow node in Rillway.
 */
public sealed interface Node permits
        StartNode,
        EndNode,
        HumanNode,
        RuleNode,
        AgentNode {

    String id();

    String name();

    NodeType type();
}
