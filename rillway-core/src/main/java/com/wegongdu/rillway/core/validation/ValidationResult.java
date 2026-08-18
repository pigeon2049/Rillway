package com.wegongdu.rillway.core.validation;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Result of validating a ProcessDefinition.
 */
public record ValidationResult(
        boolean isValid,
        List<ValidationError> errors
) implements Serializable {

    public ValidationResult {
        errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult invalid(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    public static ValidationResult invalid(ValidationError... errors) {
        return new ValidationResult(false, List.of(errors));
    }
}
