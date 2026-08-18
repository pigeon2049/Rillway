package com.wegongdu.rillway.ai.cache;

import com.wegongdu.rillway.core.model.ResolutionCache;
import java.util.List;
import java.util.Optional;

/**
 * Storage SPI for managing successful workflow resolution cache entries.
 */
public interface ResolutionCacheRepository {

    void save(ResolutionCache cache);

    Optional<ResolutionCache> findMatch(String definitionId, String nodeId, String promptHash, String departmentId, String postCode);

    List<ResolutionCache> findRecentByDefinitionAndNode(String definitionId, String nodeId, int limit);

    void delete(String id);
}
