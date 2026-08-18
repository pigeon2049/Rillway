package com.wegongdu.rillway.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a field or getter method to be excluded from workflow execution context and LLM prompts.
 * Use on sensitive data (passwords, tokens) or heavy data (byte[], files).
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProcessIgnore {
}
