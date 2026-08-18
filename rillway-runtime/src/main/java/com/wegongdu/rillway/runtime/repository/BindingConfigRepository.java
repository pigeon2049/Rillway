package com.wegongdu.rillway.runtime.repository;

import com.wegongdu.rillway.core.model.BindingConfig;
import java.util.List;
import java.util.Optional;

/**
 * Storage SPI for managing business entity and table binding configurations.
 */
public interface BindingConfigRepository {

    void save(BindingConfig config);

    Optional<BindingConfig> findByBusinessType(String businessType);

    Optional<BindingConfig> findByProcessDefinitionId(String processDefinitionId);

    List<BindingConfig> listAll();
}
