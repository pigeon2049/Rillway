package com.wegongdu.rillway.agent.registry;

import com.wegongdu.rillway.agent.spi.Agent;
import com.wegongdu.rillway.agent.spi.AgentRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of AgentRegistry.
 */
public class InMemoryAgentRegistry implements AgentRegistry {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    @Override
    public Optional<Agent> find(String agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.get(agentId));
    }

    @Override
    public void register(Agent agent) {
        if (agent != null && agent.id() != null) {
            agents.put(agent.id(), agent);
        }
    }

    @Override
    public List<Agent> listAll() {
        return List.copyOf(agents.values());
    }
}
