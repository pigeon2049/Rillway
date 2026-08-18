package com.wegongdu.rillway.runtime.repository.memory;

import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ProcessInstanceRepository.
 */
public class InMemoryProcessInstanceRepository implements ProcessInstanceRepository {

    private final Map<String, ProcessInstance> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ProcessInstance instance) {
        if (instance != null && instance.id() != null) {
            storage.put(instance.id(), instance);
        }
    }

    @Override
    public void update(ProcessInstance instance) {
        if (instance != null && instance.id() != null) {
            storage.put(instance.id(), instance);
        }
    }

    @Override
    public Optional<ProcessInstance> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<ProcessInstance> findByBusinessKey(String businessKey) {
        if (businessKey == null) return Optional.empty();
        return storage.values().stream()
                .filter(i -> businessKey.equals(i.businessKey()))
                .findFirst();
    }

    @Override
    public List<ProcessInstance> findByDefinitionId(String definitionId) {
        if (definitionId == null) return List.of();
        return storage.values().stream()
                .filter(i -> definitionId.equals(i.definitionId()))
                .toList();
    }
}
