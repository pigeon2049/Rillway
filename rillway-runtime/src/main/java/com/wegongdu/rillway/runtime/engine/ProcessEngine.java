package com.wegongdu.rillway.runtime.engine;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;

/**
 * Core workflow runtime engine for initiating and resuming process instances.
 */
public interface ProcessEngine {

    default ProcessInstance start(ProcessDefinition definition, ProcessContext context) {
        return start(definition, null, context);
    }

    ProcessInstance start(ProcessDefinition definition, String businessKey, ProcessContext context);

    default ProcessInstance startByBusinessType(String businessType, String entityId, ProcessContext context) {
        throw new UnsupportedOperationException("startByBusinessType is not supported by this engine implementation");
    }

    ProcessInstance resume(ProcessInstance instance, Decision decision);
}
