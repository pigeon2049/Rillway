package com.wegongdu.rillway.core.decision;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured explanation accompanying an Agent decision for auditing and governance.
 * <p>
 * Does not expose raw LLM chain-of-thought, but provides high-level reasoning summary,
 * evidence, policy references, and confidence score.
 */
public record AgentDecisionExplanation(
        String reasoningSummary,
        Map<String, Object> evidence,
        List<String> policyReferences,
        double confidence
) implements Serializable {

    public AgentDecisionExplanation {
        evidence = evidence != null ? Map.copyOf(evidence) : Collections.emptyMap();
        policyReferences = policyReferences != null ? List.copyOf(policyReferences) : Collections.emptyList();
    }

    public static AgentDecisionExplanation of(String reasoningSummary) {
        return new AgentDecisionExplanation(reasoningSummary, Collections.emptyMap(), Collections.emptyList(), 1.0);
    }

    public static AgentDecisionExplanation of(String reasoningSummary, List<String> policyReferences, double confidence) {
        return new AgentDecisionExplanation(reasoningSummary, Collections.emptyMap(), policyReferences, confidence);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String reasoningSummary;
        private final Map<String, Object> evidence = new java.util.HashMap<>();
        private final List<String> policyReferences = new java.util.ArrayList<>();
        private double confidence = 1.0;

        public Builder reasoningSummary(String reasoningSummary) {
            this.reasoningSummary = reasoningSummary;
            return this;
        }

        public Builder evidence(String key, Object value) {
            if (key != null && value != null) {
                this.evidence.put(key, value);
            }
            return this;
        }

        public Builder policyReferences(String... references) {
            if (references != null) {
                this.policyReferences.addAll(List.of(references));
            }
            return this;
        }

        public Builder policyReferences(List<String> references) {
            if (references != null) {
                this.policyReferences.addAll(references);
            }
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public AgentDecisionExplanation build() {
            return new AgentDecisionExplanation(reasoningSummary, evidence, policyReferences, confidence);
        }
    }
}
