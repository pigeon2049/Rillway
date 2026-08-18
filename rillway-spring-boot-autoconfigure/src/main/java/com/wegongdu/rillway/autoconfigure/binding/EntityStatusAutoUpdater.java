package com.wegongdu.rillway.autoconfigure.binding;

import com.wegongdu.rillway.audit.event.AuditEvent;
import com.wegongdu.rillway.audit.event.AuditEvents;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.BindingConfig;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

/**
 * Automatically updates business table status column upon workflow completion based on rillway_binding_config.
 */
public class EntityStatusAutoUpdater implements AuditSink {

    private static final Logger log = LoggerFactory.getLogger(EntityStatusAutoUpdater.class);

    private final JdbcTemplate jdbcTemplate;
    private final BindingConfigRepository bindingConfigRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final AuditSink delegateSink;

    public EntityStatusAutoUpdater(
            JdbcTemplate jdbcTemplate,
            BindingConfigRepository bindingConfigRepository,
            ProcessInstanceRepository instanceRepository,
            AuditSink delegateSink
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.bindingConfigRepository = bindingConfigRepository;
        this.instanceRepository = instanceRepository;
        this.delegateSink = delegateSink;
    }

    public AuditSink getDelegateSink() {
        return delegateSink;
    }

    @Override
    public void publish(AuditEvent event) {
        if (delegateSink != null) {
            delegateSink.publish(event);
        }

        if (event instanceof AuditEvents.ProcessCompleted completedEvent) {
            handleProcessCompleted(completedEvent);
        }
    }

    private void handleProcessCompleted(AuditEvents.ProcessCompleted event) {
        if (jdbcTemplate == null || bindingConfigRepository == null || instanceRepository == null) {
            return;
        }

        try {
            Optional<ProcessInstance> instanceOpt = instanceRepository.findById(event.processInstanceId());
            if (instanceOpt.isEmpty()) {
                log.warn("EntityStatusAutoUpdater: ProcessInstance [{}] not found in repository.", event.processInstanceId());
                return;
            }

            ProcessInstance instance = instanceOpt.get();
            String businessKey = instance.businessKey();
            if (businessKey == null || businessKey.isBlank()) {
                log.info("EntityStatusAutoUpdater: ProcessInstance [{}] has no businessKey, skipping auto-update.", event.processInstanceId());
                return;
            }

            // Parse businessType and entityId (e.g. "purchase_order:1001" or "1001")
            String businessType = null;
            String entityId = businessKey;
            if (businessKey.contains(":")) {
                String[] parts = businessKey.split(":", 2);
                businessType = parts[0];
                entityId = parts[1];
            }

            Optional<BindingConfig> configOpt = Optional.empty();
            if (businessType != null) {
                configOpt = bindingConfigRepository.findByBusinessType(businessType);
            }
            if (configOpt.isEmpty()) {
                configOpt = bindingConfigRepository.findByProcessDefinitionId(event.definitionId());
            }
            if (configOpt.isEmpty()) {
                configOpt = bindingConfigRepository.findByBusinessType(event.definitionId());
            }

            if (configOpt.isEmpty() || !configOpt.get().enabled()) {
                log.info("EntityStatusAutoUpdater: No matching or enabled BindingConfig for businessType [{}] or definition [{}].",
                        businessType, event.definitionId());
                return;
            }

            BindingConfig config = configOpt.get();
            String targetStatusValue = (instance.status() == ProcessStatus.COMPLETED && event.isSuccess())
                    ? config.approvedValue()
                    : config.rejectedValue();

            String updateSql = String.format(
                    "UPDATE %s SET %s = ? WHERE %s = ?",
                    config.tableName(),
                    config.statusColumn(),
                    config.primaryKeyColumn()
            );

            int updatedRows = jdbcTemplate.update(updateSql, targetStatusValue, entityId);
            log.info("EntityStatusAutoUpdater: Updated {} rows on table [{}] setting {}='{}' where {}='{}'",
                    updatedRows, config.tableName(), config.statusColumn(), targetStatusValue, config.primaryKeyColumn(), entityId);
        } catch (Exception ex) {
            log.warn("EntityStatusAutoUpdater encountered an error updating business entity status: {}", ex.getMessage(), ex);
        }
    }
}
