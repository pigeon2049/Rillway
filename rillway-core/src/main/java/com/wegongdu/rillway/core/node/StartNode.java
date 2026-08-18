package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.model.NodeType;
import java.util.Objects;

/**
 * Start node representing the entry point of a workflow process.
 */
public record StartNode(String id, String name) implements Node {

    public StartNode {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = "Start";
        }
    }

    @Override
    public NodeType type() {
        return NodeType.START;
    }

    public static StartNode of(String id) {
        return new StartNode(id, "Start");
    }

    public static StartNode of(String id, String name) {
        return new StartNode(id, name);
    }
}
