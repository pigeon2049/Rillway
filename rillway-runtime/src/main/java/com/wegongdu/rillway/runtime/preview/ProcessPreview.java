package com.wegongdu.rillway.runtime.preview;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Preview summary of a workflow execution before real start.
 */
public record ProcessPreview(
        String definitionId,
        String definitionName,
        List<String> potentialPath,
        List<String> humanApprovers,
        List<String> agentNodes,
        List<String> rules,
        List<String> requiredFormFields,
        List<String> warnings
) implements Serializable {

    public ProcessPreview {
        potentialPath = potentialPath != null ? List.copyOf(potentialPath) : Collections.emptyList();
        humanApprovers = humanApprovers != null ? List.copyOf(humanApprovers) : Collections.emptyList();
        agentNodes = agentNodes != null ? List.copyOf(agentNodes) : Collections.emptyList();
        rules = rules != null ? List.copyOf(rules) : Collections.emptyList();
        requiredFormFields = requiredFormFields != null ? List.copyOf(requiredFormFields) : Collections.emptyList();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
    }
}
