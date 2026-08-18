package com.wegongdu.rillway.policy.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Result of evaluating a policy against process context.
 */
public record PolicyResult(
        boolean passed,
        String reason,
        Map<String, Object> metadata
) implements Serializable {

    public PolicyResult {
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static PolicyResult pass(String reason) {
        return new PolicyResult(true, reason, Collections.emptyMap());
    }

    public static PolicyResult pass(String reason, Map<String, Object> metadata) {
        return new PolicyResult(true, reason, metadata);
    }

    public static PolicyResult fail(String reason) {
        return new PolicyResult(false, reason, Collections.emptyMap());
    }

    public static PolicyResult fail(String reason, Map<String, Object> metadata) {
        return new PolicyResult(false, reason, metadata);
    }
}
