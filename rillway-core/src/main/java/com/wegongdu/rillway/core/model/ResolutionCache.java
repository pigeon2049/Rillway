package com.wegongdu.rillway.core.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cached successful workflow decision/assignee resolution for Token saving and instant execution.
 */
public record ResolutionCache(
        String id,
        String definitionId,
        String nodeId,
        String promptHash,
        String departmentId,
        String postCode,
        String resolvedUserId,
        String resolvedRole,
        List<String> candidateUsers,
        List<String> candidateRoles,
        int hitCount,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    public ResolutionCache {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(definitionId, "definitionId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(promptHash, "promptHash must not be null");
        candidateUsers = candidateUsers != null ? List.copyOf(candidateUsers) : Collections.emptyList();
        candidateRoles = candidateRoles != null ? List.copyOf(candidateRoles) : Collections.emptyList();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    public ResolutionCache incrementHit() {
        return new ResolutionCache(
                id,
                definitionId,
                nodeId,
                promptHash,
                departmentId,
                postCode,
                resolvedUserId,
                resolvedRole,
                candidateUsers,
                candidateRoles,
                hitCount + 1,
                createdAt,
                Instant.now()
        );
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String definitionId;
        private String nodeId;
        private String promptHash;
        private String departmentId;
        private String postCode;
        private String resolvedUserId;
        private String resolvedRole;
        private List<String> candidateUsers;
        private List<String> candidateRoles;
        private int hitCount = 0;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder(String id) {
            this.id = id;
        }

        public Builder definitionId(String definitionId) {
            this.definitionId = definitionId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder promptHash(String promptHash) {
            this.promptHash = promptHash;
            return this;
        }

        public Builder departmentId(String departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder postCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder resolvedUserId(String resolvedUserId) {
            this.resolvedUserId = resolvedUserId;
            return this;
        }

        public Builder resolvedRole(String resolvedRole) {
            this.resolvedRole = resolvedRole;
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

        public Builder hitCount(int hitCount) {
            this.hitCount = hitCount;
            return this;
        }

        public ResolutionCache build() {
            return new ResolutionCache(
                    id,
                    definitionId,
                    nodeId,
                    promptHash,
                    departmentId,
                    postCode,
                    resolvedUserId,
                    resolvedRole,
                    candidateUsers,
                    candidateRoles,
                    hitCount,
                    createdAt,
                    updatedAt
            );
        }
    }
}
