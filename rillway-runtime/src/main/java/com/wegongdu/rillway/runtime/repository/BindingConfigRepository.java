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

    default Optional<BindingConfig> findByTableName(String tableName) {
        if (tableName == null) return Optional.empty();
        return listAll().stream()
                .filter(BindingConfig::enabled)
                .filter(c -> tableName.equalsIgnoreCase(c.tableName()))
                .findFirst();
    }

    default Optional<BindingConfig> findMatching(String identifier) {
        if (identifier == null) return Optional.empty();
        return findByBusinessType(identifier)
                .or(() -> findByTableName(identifier));
    }

    Optional<BindingConfig> findByProcessDefinitionId(String processDefinitionId);

    List<BindingConfig> listAll();
}
