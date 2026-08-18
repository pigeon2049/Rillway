package com.wegongdu.rillway.ai.cache;

import com.wegongdu.rillway.core.model.ResolutionCache;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of ResolutionCacheRepository with branch fingerprint isolation.
 */
public class InMemoryResolutionCacheRepository implements ResolutionCacheRepository {

    private final Map<String, ResolutionCache> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ResolutionCache cache) {
        if (cache != null && cache.id() != null) {
            // Remove existing match if any
            storage.values().removeIf(c ->
                    c.definitionId().equals(cache.definitionId()) &&
                    c.nodeId().equals(cache.nodeId()) &&
                    c.promptHash().equals(cache.promptHash()) &&
                    Objects.equals(c.conditionBranchKey(), cache.conditionBranchKey()) &&
                    Objects.equals(c.initiatorDeptId(), cache.initiatorDeptId()) &&
                    Objects.equals(c.initiatorPostCode(), cache.initiatorPostCode())
            );
            storage.put(cache.id(), cache);
        }
    }

    @Override
    public Optional<ResolutionCache> findMatch(
            String definitionId,
            String nodeId,
            String promptHash,
            String conditionBranchKey,
            String departmentId,
            String postCode
    ) {
        String safeBranchKey = conditionBranchKey != null ? conditionBranchKey : "DEFAULT";
        return storage.values().stream()
                .filter(c -> c.definitionId().equals(definitionId) &&
                             c.nodeId().equals(nodeId) &&
                             c.promptHash().equals(promptHash) &&
                             Objects.equals(c.conditionBranchKey(), safeBranchKey) &&
                             Objects.equals(c.initiatorDeptId(), departmentId) &&
                             Objects.equals(c.initiatorPostCode(), postCode))
                .findFirst();
    }

    @Override
    public List<ResolutionCache> findRecentByDefinitionAndNode(String definitionId, String nodeId, int limit) {
        return storage.values().stream()
                .filter(c -> c.definitionId().equals(definitionId) && c.nodeId().equals(nodeId))
                .sorted((a, b) -> b.updatedAt().compareTo(a.updatedAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            storage.remove(id);
        }
    }
}
