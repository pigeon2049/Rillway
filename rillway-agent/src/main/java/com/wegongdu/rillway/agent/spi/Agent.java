package com.wegongdu.rillway.agent.spi;

import com.wegongdu.rillway.agent.model.AgentContext;
import com.wegongdu.rillway.agent.model.AgentDecision;

/**
 * AI Agent SPI for autonomous reasoning at an AgentNode.
 */
public interface Agent {

    String id();

    AgentDecision decide(AgentContext context);
}
