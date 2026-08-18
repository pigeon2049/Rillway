package com.wegongdu.rillway.runtime.repository;

import com.wegongdu.rillway.core.instance.ProcessInstance;
import java.util.List;
import java.util.Optional;

/**
 * Storage SPI for managing persistent ProcessInstance lifecycles.
 */
public interface ProcessInstanceRepository {

    void save(ProcessInstance instance);

    void update(ProcessInstance instance);

    Optional<ProcessInstance> findById(String id);

    Optional<ProcessInstance> findByBusinessKey(String businessKey);

    List<ProcessInstance> findByDefinitionId(String definitionId);
}
