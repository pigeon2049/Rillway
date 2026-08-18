package com.wegongdu.rillway.agent.spi;

import java.util.List;
import java.util.Optional;

/**
 * Registry interface for discovering and managing Agents.
 */
public interface AgentRegistry {

    Optional<Agent> find(String agentId);

    void register(Agent agent);

    List<Agent> listAll();
}
