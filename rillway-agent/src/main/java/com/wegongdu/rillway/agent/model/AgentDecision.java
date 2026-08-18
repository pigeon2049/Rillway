package com.wegongdu.rillway.agent.model;

import com.wegongdu.rillway.core.decision.Decision;
import java.io.Serializable;

/**
 * Output of an Agent inference execution.
 */
public record AgentDecision(
        Decision decision,
        double confidence,
        boolean needsFallback,
        String fallbackReason
) implements Serializable {

    public static AgentDecision of(Decision decision) {
        return new AgentDecision(decision, 1.0, false, null);
    }

    public static AgentDecision of(Decision decision, double confidence) {
        return new AgentDecision(decision, confidence, false, null);
    }

    public static AgentDecision fallback(String fallbackReason) {
        return new AgentDecision(null, 0.0, true, fallbackReason);
    }

    public static AgentDecision fallback(String fallbackReason, double confidence) {
        return new AgentDecision(null, confidence, true, fallbackReason);
    }
}
