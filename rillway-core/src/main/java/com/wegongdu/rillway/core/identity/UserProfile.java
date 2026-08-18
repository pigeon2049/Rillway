package com.wegongdu.rillway.core.identity;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Multi-dimensional identity and organizational profile of a user (initiator, approver, etc.).
 */
public record UserProfile(
        String userId,
        String username,
        String departmentId,
        String departmentName,
        String postCode,
        String postName,
        List<String> roles,
        String directLeaderId,
        Map<String, Object> extraAttributes
) implements Serializable {

    public UserProfile {
        Objects.requireNonNull(userId, "userId must not be null");
        if (username == null || username.isBlank()) {
            username = userId;
        }
        roles = roles != null ? List.copyOf(roles) : Collections.emptyList();
        extraAttributes = extraAttributes != null ? Map.copyOf(extraAttributes) : Collections.emptyMap();
    }

    public static Builder builder(String userId) {
        return new Builder(userId);
    }

    public static final class Builder {
        private final String userId;
        private String username;
        private String departmentId;
        private String departmentName;
        private String postCode;
        private String postName;
        private List<String> roles;
        private String directLeaderId;
        private Map<String, Object> extraAttributes;

        public Builder(String userId) {
            this.userId = userId;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder departmentId(String departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public Builder postCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder postName(String postName) {
            this.postName = postName;
            return this;
        }

        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public Builder directLeaderId(String directLeaderId) {
            this.directLeaderId = directLeaderId;
            return this;
        }

        public Builder extraAttributes(Map<String, Object> extraAttributes) {
            this.extraAttributes = extraAttributes;
            return this;
        }

        public UserProfile build() {
            return new UserProfile(
                    userId,
                    username,
                    departmentId,
                    departmentName,
                    postCode,
                    postName,
                    roles,
                    directLeaderId,
                    extraAttributes
            );
        }
    }
}
