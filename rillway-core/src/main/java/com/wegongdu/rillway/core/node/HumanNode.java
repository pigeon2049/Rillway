package com.wegongdu.rillway.core.node;

import com.wegongdu.rillway.core.model.NodeType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Human decision/approval node.
 */
public record HumanNode(
        String id,
        String name,
        String assigneeUser,
        String assigneeRole,
        List<String> candidateUsers,
        List<String> candidateRoles
) implements Node {

    public HumanNode {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            name = "Human Approval";
        }
        candidateUsers = candidateUsers != null ? List.copyOf(candidateUsers) : Collections.emptyList();
        candidateRoles = candidateRoles != null ? List.copyOf(candidateRoles) : Collections.emptyList();
    }

    @Override
    public NodeType type() {
        return NodeType.HUMAN;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String name;
        private String assigneeUser;
        private String assigneeRole;
        private List<String> candidateUsers;
        private List<String> candidateRoles;

        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder assigneeUser(String assigneeUser) {
            this.assigneeUser = assigneeUser;
            return this;
        }

        public Builder assigneeRole(String assigneeRole) {
            this.assigneeRole = assigneeRole;
            return this;
        }

        public Builder candidateUsers(List<String> candidateUsers) {
            this.candidateUsers = candidateUsers;
            return this;
        }

        public Builder candidateRoles(List<String> candidateRoles) {
            this.candidateRoles = candidateRoles;
            return this;
        }

        public HumanNode build() {
            return new HumanNode(id, name, assigneeUser, assigneeRole, candidateUsers, candidateRoles);
        }
    }
}
