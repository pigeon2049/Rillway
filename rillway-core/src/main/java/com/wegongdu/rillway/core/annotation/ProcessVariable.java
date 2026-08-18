package com.wegongdu.rillway.core.annotation;

import java.lang.annotation.*;

/**
 * Customizes the variable name when mapped to workflow ProcessContext.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProcessVariable {

    /**
     * Variable name alias in ProcessContext.
     */
    String value() default "";
}
