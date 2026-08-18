package com.wegongdu.rillway.core.definition;

import com.wegongdu.rillway.core.model.NodeType;
import com.wegongdu.rillway.core.node.AgentNode;
import com.wegongdu.rillway.core.node.EndNode;
import com.wegongdu.rillway.core.node.HumanNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.node.RuleNode;
import com.wegongdu.rillway.core.node.StartNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Immutable definition of a workflow process.
 */
public record ProcessDefinition(
        String id,
        String name,
        String version,
        String description,
        String startNodeId,
        Map<String, Node> nodes,
        List<Edge> edges
) implements Serializable {

    public ProcessDefinition {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = id;
        }
        if (version == null || version.isBlank()) {
            version = "1.0.0";
        }
        nodes = nodes != null ? Map.copyOf(new LinkedHashMap<>(nodes)) : Collections.emptyMap();
        edges = edges != null ? List.copyOf(edges) : Collections.emptyList();
    }

    public Optional<Node> findNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Node getNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found in definition: " + nodeId);
        }
        return node;
    }

    public StartNode getStartNode() {
        if (startNodeId == null) {
            return (StartNode) nodes.values().stream()
                    .filter(n -> n.type() == NodeType.START)
                    .findFirst()
                    .orElse(null);
        }
        Node node = nodes.get(startNodeId);
        return node instanceof StartNode sn ? sn : null;
    }

    public List<Edge> outgoingEdges(String sourceNodeId) {
        return edges.stream()
                .filter(e -> e.sourceNodeId().equals(sourceNodeId))
                .toList();
    }

    /**
     * Checks whether the specified node directly connects only to End nodes (or has no outgoing edges),
     * indicating that approving this node will complete the process.
     */
    public boolean isTerminalNode(String nodeId) {
        if (nodeId == null) return false;
        List<Edge> out = outgoingEdges(nodeId);
        if (out.isEmpty()) return true;
        return out.stream().allMatch(edge -> {
            Node target = nodes.get(edge.targetNodeId());
            return target != null && target.type() == NodeType.END;
        });
    }

    /**
     * Alias for isTerminalNode(nodeId).
     */
    public boolean isFinalApproval(String nodeId) {
        return isTerminalNode(nodeId);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String name;
        private String version = "1.0.0";
        private String description;
        private String startNodeId;
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final List<Edge> edges = new ArrayList<>();

        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder startNode(String id) {
            return startNode(id, "Start");
        }

        public Builder startNode(String id, String name) {
            StartNode node = new StartNode(id, name);
            this.nodes.put(id, node);
            if (this.startNodeId == null) {
                this.startNodeId = id;
            }
            return this;
        }

        public Builder endNode(String id) {
            return endNode(id, "End", true);
        }

        public Builder endNode(String id, String name) {
            return endNode(id, name, true);
        }

        public Builder endNode(String id, String name, boolean isSuccess) {
            EndNode node = new EndNode(id, name, isSuccess);
            this.nodes.put(id, node);
            return this;
        }

        public Builder humanNode(String id, Consumer<HumanNode.Builder> consumer) {
            HumanNode.Builder b = HumanNode.builder(id);
            consumer.accept(b);
            this.nodes.put(id, b.build());
            return this;
        }

        public Builder ruleNode(String id, Consumer<RuleNode.Builder> consumer) {
            RuleNode.Builder b = RuleNode.builder(id);
            consumer.accept(b);
            this.nodes.put(id, b.build());
            return this;
        }

        public Builder agentNode(String id, Consumer<AgentNode.Builder> consumer) {
            AgentNode.Builder b = AgentNode.builder(id);
            consumer.accept(b);
            this.nodes.put(id, b.build());
            return this;
        }

        public Builder node(Node node) {
            Objects.requireNonNull(node, "node must not be null");
            this.nodes.put(node.id(), node);
            if (node instanceof StartNode && this.startNodeId == null) {
                this.startNodeId = node.id();
            }
            return this;
        }

        public Builder edge(String sourceNodeId, String targetNodeId) {
            this.edges.add(Edge.of(sourceNodeId, targetNodeId));
            return this;
        }

        public Builder edge(Edge edge) {
            Objects.requireNonNull(edge, "edge must not be null");
            this.edges.add(edge);
            return this;
        }

        public ProcessDefinition build() {
            return new ProcessDefinition(id, name, version, description, startNodeId, nodes, edges);
        }
    }
}
