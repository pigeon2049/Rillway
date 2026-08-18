package com.wegongdu.rillway.core.annotation;

import java.lang.annotation.*;

/**
 * Marks a field or method as the primary key/ID of a workflow entity.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityId {
}
