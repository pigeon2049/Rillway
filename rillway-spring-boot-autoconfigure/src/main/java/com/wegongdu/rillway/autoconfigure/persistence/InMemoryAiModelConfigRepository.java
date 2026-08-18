package com.wegongdu.rillway.autoconfigure.persistence;

import com.wegongdu.rillway.ai.config.AiModelConfig;
import com.wegongdu.rillway.ai.config.AiModelConfigRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fallback implementation of AiModelConfigRepository.
 */
public class InMemoryAiModelConfigRepository implements AiModelConfigRepository {

    private final Map<String, AiModelConfig> store = new ConcurrentHashMap<>();

    @Override
    public void save(AiModelConfig config) {
        if (config == null || config.id() == null) return;
        if (config.isDefault()) {
            store.replaceAll((k, v) -> v.isDefault() ? AiModelConfig.builder(v.id())
                    .providerName(v.providerName())
                    .baseUrl(v.baseUrl())
                    .apiKey(v.apiKey())
                    .modelName(v.modelName())
                    .temperature(v.temperature())
                    .timeoutSeconds(v.timeoutSeconds())
                    .isDefault(false)
                    .enabled(v.enabled())
                    .updatedAt(v.updatedAt())
                    .build() : v);
        }
        store.put(config.id(), config);
    }

    @Override
    public Optional<AiModelConfig> findById(String id) {
        if (id == null) return Optional.empty();
        AiModelConfig cfg = store.get(id);
        return (cfg != null && cfg.enabled()) ? Optional.of(cfg) : Optional.empty();
    }

    @Override
    public Optional<AiModelConfig> findDefault() {
        return store.values().stream()
                .filter(AiModelConfig::enabled)
                .filter(AiModelConfig::isDefault)
                .findFirst()
                .or(() -> store.values().stream().filter(AiModelConfig::enabled).findFirst());
    }

    @Override
    public Optional<AiModelConfig> findByProvider(String providerName) {
        if (providerName == null) return Optional.empty();
        return store.values().stream()
                .filter(AiModelConfig::enabled)
                .filter(c -> providerName.equalsIgnoreCase(c.providerName()))
                .findFirst();
    }

    @Override
    public List<AiModelConfig> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            store.remove(id);
        }
    }
}
