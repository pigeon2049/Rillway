package com.wegongdu.rillway.runtime.executor;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import java.io.Serializable;
import java.util.Objects;

/**
 * Context passed to a NodeExecutor during node execution.
 */
public record ExecutionContext(
        ProcessDefinition definition,
        ProcessInstance instance,
        ProcessContext context,
        Decision inputDecision
) implements Serializable {

    public ExecutionContext {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(instance, "instance must not be null");
        if (context == null) {
            context = instance.context();
        }
    }

    public static ExecutionContext of(ProcessDefinition definition, ProcessInstance instance) {
        return new ExecutionContext(definition, instance, instance.context(), null);
    }

    public static ExecutionContext of(ProcessDefinition definition, ProcessInstance instance, Decision inputDecision) {
        return new ExecutionContext(definition, instance, instance.context(), inputDecision);
    }
}
