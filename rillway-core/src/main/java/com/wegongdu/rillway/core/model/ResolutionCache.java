package com.wegongdu.rillway.core.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cached successful workflow decision snapshot with TTL and organizational state fingerprints.
 */
public record ResolutionCache(
        String id,
        String definitionId,
        String nodeId,
        String promptHash,

        // Initiator snapshot when resolved
        String initiatorUserId,
        String initiatorDeptId,
        String initiatorPostCode,

        // Resolved assignee snapshot when resolved
        String resolvedUserId,
        String resolvedDeptId,
        String resolvedPostCode,
        String resolvedRole,
        List<String> candidateUsers,
        List<String> candidateRoles,

        int hitCount,
        Instant expiresAt,
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
        if (expiresAt == null) expiresAt = createdAt.plus(7, ChronoUnit.DAYS); // default 7 days TTL
    }

    public ResolutionCache incrementHit() {
        return new ResolutionCache(
                id,
                definitionId,
                nodeId,
                promptHash,
                initiatorUserId,
                initiatorDeptId,
                initiatorPostCode,
                resolvedUserId,
                resolvedDeptId,
                resolvedPostCode,
                resolvedRole,
                candidateUsers,
                candidateRoles,
                hitCount + 1,
                expiresAt,
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
        private String initiatorUserId;
        private String initiatorDeptId;
        private String initiatorPostCode;
        private String resolvedUserId;
        private String resolvedDeptId;
        private String resolvedPostCode;
        private String resolvedRole;
        private List<String> candidateUsers;
        private List<String> candidateRoles;
        private int hitCount = 0;
        private Instant expiresAt;
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

        public Builder initiatorUserId(String initiatorUserId) {
            this.initiatorUserId = initiatorUserId;
            return this;
        }

        public Builder initiatorDeptId(String initiatorDeptId) {
            this.initiatorDeptId = initiatorDeptId;
            return this;
        }

        public Builder initiatorPostCode(String initiatorPostCode) {
            this.initiatorPostCode = initiatorPostCode;
            return this;
        }

        public Builder resolvedUserId(String resolvedUserId) {
            this.resolvedUserId = resolvedUserId;
            return this;
        }

        public Builder resolvedDeptId(String resolvedDeptId) {
            this.resolvedDeptId = resolvedDeptId;
            return this;
        }

        public Builder resolvedPostCode(String resolvedPostCode) {
            this.resolvedPostCode = resolvedPostCode;
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

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ResolutionCache build() {
            return new ResolutionCache(
                    id,
                    definitionId,
                    nodeId,
                    promptHash,
                    initiatorUserId,
                    initiatorDeptId,
                    initiatorPostCode,
                    resolvedUserId,
                    resolvedDeptId,
                    resolvedPostCode,
                    resolvedRole,
                    candidateUsers,
                    candidateRoles,
                    hitCount,
                    expiresAt,
                    createdAt,
                    updatedAt
            );
        }
    }
}
