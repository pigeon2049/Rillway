package com.wegongdu.rillway.runtime.repository.memory;

import com.wegongdu.rillway.core.instance.ExecutionRecord;
import com.wegongdu.rillway.runtime.repository.ExecutionHistoryRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of ExecutionHistoryRepository.
 */
public class InMemoryExecutionHistoryRepository implements ExecutionHistoryRepository {

    private final Map<String, List<ExecutionRecord>> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String processInstanceId, ExecutionRecord record) {
        if (processInstanceId != null && record != null) {
            storage.computeIfAbsent(processInstanceId, k -> new CopyOnWriteArrayList<>()).add(record);
        }
    }

    @Override
    public List<ExecutionRecord> findByProcessInstanceId(String processInstanceId) {
        if (processInstanceId == null) return List.of();
        List<ExecutionRecord> list = storage.get(processInstanceId);
        return list != null ? Collections.unmodifiableList(list) : List.of();
    }
}
