package com.wegongdu.rillway.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Configuration mapping a business entity/table to a workflow definition and its status callback values.
 * Supports natural language processPrompt for zero-code dynamic AI intent-driven workflows.
 */
public record BindingConfig(
        String id,
        String businessType,
        String processDefinitionId,
        String processPrompt,
        String tableName,
        String primaryKeyColumn,
        String statusColumn,
        String approvedValue,
        String rejectedValue,
        String runningValue,
        boolean enabled
) implements Serializable {

    public BindingConfig {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(businessType, "businessType must not be null");
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            processDefinitionId = businessType + "-workflow";
        }
        if (tableName == null || tableName.isBlank()) {
            tableName = businessType;
        }
        if (primaryKeyColumn == null || primaryKeyColumn.isBlank()) {
            primaryKeyColumn = "id";
        }
        if (statusColumn == null || statusColumn.isBlank()) {
            statusColumn = "status";
        }
        if (approvedValue == null || approvedValue.isBlank()) {
            approvedValue = "APPROVED";
        }
        if (rejectedValue == null || rejectedValue.isBlank()) {
            rejectedValue = "REJECTED";
        }
    }

    public static BindingConfig of(
            String id,
            String businessType,
            String processDefinitionId,
            String tableName,
            String statusColumn,
            String approvedValue,
            String rejectedValue
    ) {
        return new BindingConfig(
                id,
                businessType,
                processDefinitionId,
                null,
                tableName,
                "id",
                statusColumn,
                approvedValue,
                rejectedValue,
                "PROCESSING",
                true
        );
    }

    public static BindingConfig ofPrompt(
            String id,
            String businessType,
            String processPrompt,
            String tableName,
            String statusColumn,
            String approvedValue,
            String rejectedValue
    ) {
        return new BindingConfig(
                id,
                businessType,
                businessType + "-workflow",
                processPrompt,
                tableName,
                "id",
                statusColumn,
                approvedValue,
                rejectedValue,
                "PROCESSING",
                true
        );
    }
}
