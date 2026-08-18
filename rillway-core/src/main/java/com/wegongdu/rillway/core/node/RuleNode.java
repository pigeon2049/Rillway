package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.model.NodeType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Deterministic rule-based evaluation and routing node.
 */
public record RuleNode(
        String id,
        String name,
        List<RuleBranch> branches,
        String defaultTargetNodeId
) implements Node {

    public RuleNode {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = "Rule Routing";
        }
        branches = branches != null ? List.copyOf(branches) : Collections.emptyList();
    }

    @Override
    public NodeType type() {
        return NodeType.RULE;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public record RuleBranch(
            String description,
            Predicate<ProcessContext> condition,
            String targetNodeId
    ) implements Serializable {
        public RuleBranch {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        }
    }

    public static final class Builder {
        private final String id;
        private String name;
        private final List<RuleBranch> branches = new ArrayList<>();
        private String defaultTargetNodeId;

        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder when(Predicate<ProcessContext> condition, String targetNodeId) {
            return when("Rule condition", condition, targetNodeId);
        }

        public Builder when(String description, Predicate<ProcessContext> condition, String targetNodeId) {
            this.branches.add(new RuleBranch(description, condition, targetNodeId));
            return this;
        }

        public Builder otherwise(String defaultTargetNodeId) {
            this.defaultTargetNodeId = defaultTargetNodeId;
            return this;
        }

        public RuleNode build() {
            return new RuleNode(id, name, branches, defaultTargetNodeId);
        }
    }
}
