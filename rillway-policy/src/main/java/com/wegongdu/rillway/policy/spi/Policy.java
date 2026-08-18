package com.wegongdu.rillway.policy.spi;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.policy.model.PolicyResult;

/**
 * Enterprise policy evaluation SPI.
 */
public interface Policy {

    String id();

    String name();

    PolicyResult evaluate(ProcessContext context);
}
