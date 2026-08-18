package com.wegongdu.rillway.runtime.engine;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;

/**
 * Core workflow runtime engine for initiating and resuming process instances.
 */
public interface ProcessEngine {

    ProcessInstance start(ProcessDefinition definition, ProcessContext context);

    ProcessInstance resume(ProcessInstance instance, Decision decision);
}
