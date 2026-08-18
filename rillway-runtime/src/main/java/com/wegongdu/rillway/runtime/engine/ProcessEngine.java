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

    default ProcessInstance start(ProcessDefinition definition, Object entityBean) {
        String entityId = com.wegongdu.rillway.core.util.EntityBeanResolver.resolveEntityId(entityBean);
        String businessType = com.wegongdu.rillway.core.util.EntityBeanResolver.resolveBusinessType(entityBean != null ? entityBean.getClass() : null);
        String businessKey = (businessType != null && entityId != null) ? (businessType + ":" + entityId) : entityId;
        return start(definition, businessKey, com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    default ProcessInstance start(ProcessDefinition definition, String businessKey, Object entityBean) {
        return start(definition, businessKey, (entityBean instanceof ProcessContext ctx) ? ctx : com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    ProcessInstance start(ProcessDefinition definition, String businessKey, ProcessContext context);

    default ProcessInstance start(Object entityBean) {
        if (entityBean == null) {
            throw new IllegalArgumentException("entityBean must not be null");
        }
        if (com.wegongdu.rillway.core.util.EntityBeanResolver.isInvalidEntity(entityBean)) {
            throw new IllegalArgumentException(
                    "[Rillway] Object of type [" + entityBean.getClass().getName() + "] is not a valid workflow entity! " +
                    "Pass a JavaBean/Record or use: processEngine.startByBusinessType(businessType, entityId, context);"
            );
        }
        String businessType = com.wegongdu.rillway.core.util.EntityBeanResolver.resolveBusinessType(entityBean.getClass());
        String entityId = com.wegongdu.rillway.core.util.EntityBeanResolver.resolveEntityId(entityBean);
        if (entityId == null || entityId.isBlank()) {
            entityId = java.util.UUID.randomUUID().toString();
        }
        return startByBusinessType(businessType, entityId, com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    default ProcessInstance start(String businessType, Object entityBean) {
        if (entityBean == null) {
            throw new IllegalArgumentException("entityBean must not be null");
        }
        String entityId = com.wegongdu.rillway.core.util.EntityBeanResolver.resolveEntityId(entityBean);
        if (entityId == null || entityId.isBlank()) {
            entityId = java.util.UUID.randomUUID().toString();
        }
        return startByBusinessType(businessType, entityId, com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    default ProcessInstance start(String businessType, String entityId, Object entityBean) {
        return startByBusinessType(businessType, entityId, (entityBean instanceof ProcessContext ctx) ? ctx : com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    default ProcessInstance startByBusinessType(String businessType, String entityId, Object entityBean) {
        return startByBusinessType(businessType, entityId, (entityBean instanceof ProcessContext ctx) ? ctx : com.wegongdu.rillway.core.context.ProcessContext.from(entityBean));
    }

    default ProcessInstance startByBusinessType(String businessType, String entityId, ProcessContext context) {
        throw new UnsupportedOperationException("startByBusinessType is not supported by this engine implementation");
    }

    ProcessInstance resume(ProcessInstance instance, Decision decision);

    default java.util.Optional<ProcessDefinition> findDefinition(String definitionId) {
        return java.util.Optional.empty();
    }
}
