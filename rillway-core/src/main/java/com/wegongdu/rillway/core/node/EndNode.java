package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.model.NodeType;
import java.util.Objects;

/**
 * End node representing the termination of a workflow branch or process.
 */
public record EndNode(String id, String name, boolean isSuccess) implements Node {

    public EndNode {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = "End";
        }
    }

    @Override
    public NodeType type() {
        return NodeType.END;
    }

    public static EndNode of(String id) {
        return new EndNode(id, "End", true);
    }

    public static EndNode of(String id, String name) {
        return new EndNode(id, name, true);
    }

    public static EndNode of(String id, String name, boolean isSuccess) {
        return new EndNode(id, name, isSuccess);
    }
}
