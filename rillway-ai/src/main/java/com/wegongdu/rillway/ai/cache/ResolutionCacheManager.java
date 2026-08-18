package com.wegongdu.rillway.ai.cache;

import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.model.ResolutionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages decision cache matching, identity verification, and few-shot reference extraction.
 */
public class ResolutionCacheManager {

    private static final Logger log = LoggerFactory.getLogger(ResolutionCacheManager.class);

    private final ResolutionCacheRepository cacheRepository;

    public ResolutionCacheManager(ResolutionCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository != null ? cacheRepository : new InMemoryResolutionCacheRepository();
    }

    /**
     * Checks if a valid cached resolution exists and verifies its current accuracy against IdentityService.
     */
    public Optional<HumanAssigneeResolver.ResolvedAssignee> findValidAssignee(
            String definitionId,
            String nodeId,
            String prompt,
            UserProfile userProfile,
            IdentityService identityService
    ) {
        if (definitionId == null || nodeId == null || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        String promptHash = computePromptHash(prompt);
        String deptId = userProfile != null ? userProfile.departmentId() : null;
        String postCode = userProfile != null ? userProfile.postCode() : null;

        Optional<ResolutionCache> cacheOpt = cacheRepository.findMatch(definitionId, nodeId, promptHash, deptId, postCode);
        if (cacheOpt.isEmpty()) {
            return Optional.empty();
        }

        ResolutionCache cached = cacheOpt.get();
        boolean verified = verifyCachedAccuracy(prompt, userProfile, cached, identityService);

        if (verified) {
            log.info("ResolutionCache HIT [0 Token]: Node [{}] matched cached assignee [{}] (Hits: {})",
                    nodeId, cached.resolvedUserId(), cached.hitCount() + 1);
            cacheRepository.save(cached.incrementHit());
            return Optional.of(HumanAssigneeResolver.ResolvedAssignee.of(
                    cached.resolvedUserId(),
                    cached.resolvedRole(),
                    cached.candidateUsers(),
                    cached.candidateRoles()
            ));
        } else {
            log.warn("ResolutionCache INVALIDATED due to organizational change. Node [{}], stale assignee [{}]",
                    nodeId, cached.resolvedUserId());
            cacheRepository.delete(cached.id());
            return Optional.empty();
        }
    }

    /**
     * Saves a newly resolved decision into the cache for future zero-token execution.
     */
    public void recordSuccessfulResolution(
            String definitionId,
            String nodeId,
            String prompt,
            UserProfile userProfile,
            HumanAssigneeResolver.ResolvedAssignee resolved
    ) {
        if (definitionId == null || nodeId == null || prompt == null || resolved == null) {
            return;
        }

        String promptHash = computePromptHash(prompt);
        String deptId = userProfile != null ? userProfile.departmentId() : null;
        String postCode = userProfile != null ? userProfile.postCode() : null;

        ResolutionCache cache = ResolutionCache.builder(UUID.randomUUID().toString())
                .definitionId(definitionId)
                .nodeId(nodeId)
                .promptHash(promptHash)
                .departmentId(deptId)
                .postCode(postCode)
                .resolvedUserId(resolved.assigneeUser())
                .resolvedRole(resolved.assigneeRole())
                .candidateUsers(resolved.candidateUsers())
                .candidateRoles(resolved.candidateRoles())
                .hitCount(0)
                .build();

        cacheRepository.save(cache);
        log.info("ResolutionCache RECORDED for Node [{}] -> Assignee [{}]", nodeId, resolved.assigneeUser());
    }

    /**
     * Retrieves recent successful examples to provide Few-Shot guidance for LLM.
     */
    public List<ResolutionCache> getRecentExamples(String definitionId, String nodeId, int limit) {
        if (definitionId == null || nodeId == null) return List.of();
        return cacheRepository.findRecentByDefinitionAndNode(definitionId, nodeId, limit);
    }

    private boolean verifyCachedAccuracy(
            String prompt,
            UserProfile userProfile,
            ResolutionCache cached,
            IdentityService identityService
    ) {
        if (identityService == null || cached.resolvedUserId() == null) {
            return true; // no SPI available, accept cache
        }

        String lowerPrompt = prompt.toLowerCase();

        // 1. If prompt refers to department head/manager
        if (lowerPrompt.contains("主管") || lowerPrompt.contains("部门负责人") || lowerPrompt.contains("部门经理")) {
            if (userProfile != null && userProfile.departmentId() != null) {
                Optional<String> currentMgr = identityService.getDepartmentManager(userProfile.departmentId());
                return currentMgr.isPresent() && currentMgr.get().equals(cached.resolvedUserId());
            }
        }

        // 2. If prompt refers to direct leader
        if (lowerPrompt.contains("领导") || lowerPrompt.contains("上级") || lowerPrompt.contains("leader")) {
            if (userProfile != null) {
                Optional<String> currentLeader = identityService.getDirectLeader(userProfile.userId());
                return currentLeader.isPresent() && currentLeader.get().equals(cached.resolvedUserId());
            }
        }

        // 3. Fallback check if user still exists in system
        return identityService.getUserProfile(cached.resolvedUserId()).isPresent() ||
               identityService.getDirectLeader(cached.resolvedUserId()).isPresent();
    }

    public static String computePromptHash(String prompt) {
        if (prompt == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(prompt.trim().hashCode());
        }
    }
}
