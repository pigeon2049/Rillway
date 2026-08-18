package com.wegongdu.rillway.runtime.repository.memory;

import com.wegongdu.rillway.core.model.BindingConfig;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of BindingConfigRepository.
 */
public class InMemoryBindingConfigRepository implements BindingConfigRepository {

    private final Map<String, BindingConfig> storage = new ConcurrentHashMap<>();

    @Override
    public void save(BindingConfig config) {
        if (config != null && config.id() != null) {
            storage.put(config.id(), config);
        }
    }

    @Override
    public Optional<BindingConfig> findByBusinessType(String businessType) {
        if (businessType == null) return Optional.empty();
        return storage.values().stream()
                .filter(c -> businessType.equalsIgnoreCase(c.businessType()))
                .findFirst();
    }

    @Override
    public Optional<BindingConfig> findByProcessDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) return Optional.empty();
        return storage.values().stream()
                .filter(c -> processDefinitionId.equals(c.processDefinitionId()))
                .findFirst();
    }

    @Override
    public List<BindingConfig> listAll() {
        return List.copyOf(storage.values());
    }
}
