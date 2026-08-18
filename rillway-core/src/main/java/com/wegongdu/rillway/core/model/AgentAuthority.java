package com.wegongdu.rillway.core.model;

/**
 * Declares the boundary of authority granted to an AI Agent at a specific node.
 * <p>
 * This is a domain constraint verified by the runtime engine, not merely a prompt instruction.
 */
public enum AgentAuthority {

    /**
     * Advisory authority: The agent can only provide recommendations, reasoning, and analysis.
     * The final decision must be made or confirmed by a Human actor.
     */
    ADVISORY,

    /**
     * Delegated authority: The agent is authorized to make direct decisions (e.g. approve/reject/escalate)
     * within predefined decision types and constraints.
     */
    DELEGATED,

    /**
     * Autonomous authority: The agent is authorized to make direct decisions and dynamically choose the subsequent routing path.
     */
    AUTONOMOUS
}
