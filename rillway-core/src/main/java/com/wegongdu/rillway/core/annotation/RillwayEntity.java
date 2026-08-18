package com.wegongdu.rillway.core.annotation;

import java.lang.annotation.*;

/**
 * Annotation to customize workflow entity metadata on a business POJO / DTO / Entity class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RillwayEntity {

    /**
     * Business type matching rillway_binding_config.business_type or table_name.
     */
    String value() default "";

    /**
     * Alias for value.
     */
    String businessType() default "";

    /**
     * Primary key property name (e.g. "id", "orderNo").
     */
    String idField() default "";

    /**
     * Initiator property name (e.g. "initiator", "creator", "applicant").
     */
    String initiatorField() default "";
}
