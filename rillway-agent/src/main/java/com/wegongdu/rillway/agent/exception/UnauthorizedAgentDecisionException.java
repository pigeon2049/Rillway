package com.wegongdu.rillway.agent.exception;

/**
 * Exception thrown when an Agent attempts to execute an action violating its declared authority or allowed decisions.
 */
public class UnauthorizedAgentDecisionException extends RuntimeException {

    private final String agentId;
    private final String nodeId;

    public UnauthorizedAgentDecisionException(String agentId, String nodeId, String message) {
        super(String.format("Agent [%s] on node [%s] violated authority: %s", agentId, nodeId, message));
        this.agentId = agentId;
        this.nodeId = nodeId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
