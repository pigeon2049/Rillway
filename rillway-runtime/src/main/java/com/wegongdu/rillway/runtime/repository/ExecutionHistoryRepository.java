package com.wegongdu.rillway.runtime.repository;

import com.wegongdu.rillway.core.instance.ExecutionRecord;
import java.util.List;

/**
 * Storage SPI for managing process execution history trails.
 */
public interface ExecutionHistoryRepository {

    void save(String processInstanceId, ExecutionRecord record);

    List<ExecutionRecord> findByProcessInstanceId(String processInstanceId);
}
