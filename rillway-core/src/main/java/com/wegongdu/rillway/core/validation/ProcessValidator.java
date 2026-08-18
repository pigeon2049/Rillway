package com.wegongdu.rillway.core.validation;

import com.wegongdu.rillway.core.definition.ProcessDefinition;

/**
 * Validator interface for ProcessDefinitions.
 */
public interface ProcessValidator {

    ValidationResult validate(ProcessDefinition definition);
}
