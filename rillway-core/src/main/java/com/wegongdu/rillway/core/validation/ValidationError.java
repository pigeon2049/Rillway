package com.wegongdu.rillway.core.validation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Validation error details.
 */
public record ValidationError(
        String code,
        String message,
        String nodeId
) implements Serializable {

    public ValidationError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public static ValidationError of(String code, String message) {
        return new ValidationError(code, message, null);
    }

    public static ValidationError of(String code, String message, String nodeId) {
        return new ValidationError(code, message, nodeId);
    }
}
