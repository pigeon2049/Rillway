package com.wegongdu.rillway.ai.config;

import java.util.List;
import java.util.Optional;

/**
 * SPI for managing AI model configurations.
 */
public interface AiModelConfigRepository {

    void save(AiModelConfig config);

    Optional<AiModelConfig> findById(String id);

    Optional<AiModelConfig> findDefault();

    Optional<AiModelConfig> findByProvider(String providerName);

    List<AiModelConfig> findAll();

    void deleteById(String id);
}
