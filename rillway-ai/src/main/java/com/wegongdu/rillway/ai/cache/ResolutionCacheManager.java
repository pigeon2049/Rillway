package com.wegongdu.rillway.ai.cache;

import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.model.ResolutionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages decision cache matching with condition branch isolation, organizational snapshot verification, and TTL.
 */
public class ResolutionCacheManager {

    private static final Logger log = LoggerFactory.getLogger(ResolutionCacheManager.class);

    private final ResolutionCacheRepository cacheRepository;
    private final Duration defaultTtl;

    public ResolutionCacheManager(ResolutionCacheRepository cacheRepository) {
        this(cacheRepository, Duration.ofDays(7));
    }

    public ResolutionCacheManager(ResolutionCacheRepository cacheRepository, Duration defaultTtl) {
        this.cacheRepository = cacheRepository != null ? cacheRepository : new InMemoryResolutionCacheRepository();
        this.defaultTtl = defaultTtl != null ? defaultTtl : Duration.ofDays(7);
    }

    /**
     * Checks if a valid cached decision snapshot exists under the specific condition branch
     * and verifies its current accuracy against IdentityService.
     */
    public Optional<HumanAssigneeResolver.ResolvedAssignee> findValidAssignee(
            String definitionId,
            String nodeId,
            String prompt,
            String conditionBranchKey,
            UserProfile currentInitiator,
            IdentityService identityService
    ) {
        if (definitionId == null || nodeId == null || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        String promptHash = computePromptHash(prompt);
        String initiatorDeptId = currentInitiator != null ? currentInitiator.departmentId() : null;
        String initiatorPostCode = currentInitiator != null ? currentInitiator.postCode() : null;
        String safeBranchKey = conditionBranchKey != null ? conditionBranchKey : "DEFAULT";

        Optional<ResolutionCache> cacheOpt = cacheRepository.findMatch(
                definitionId, nodeId, promptHash, safeBranchKey, initiatorDeptId, initiatorPostCode
        );

        if (cacheOpt.isEmpty()) {
            return Optional.empty();
        }

        ResolutionCache cached = cacheOpt.get();
        boolean verified = verifySnapshotAccuracy(currentInitiator, cached, identityService);

        if (verified) {
            log.info("ResolutionCache HIT [0 Token]: Node [{}], Branch [{}] matched cached assignee [{}] (Hits: {}, Valid until: {})",
                    nodeId, safeBranchKey, cached.resolvedUserId(), cached.hitCount() + 1, cached.expiresAt());
            cacheRepository.save(cached.incrementHit());
            return Optional.of(HumanAssigneeResolver.ResolvedAssignee.of(
                    cached.resolvedUserId(),
                    cached.resolvedRole(),
                    cached.candidateUsers(),
                    cached.candidateRoles()
            ));
        } else {
            log.warn("ResolutionCache INVALIDATED (Expired or organizational profile changed). Node [{}], Branch [{}], stale assignee [{}]",
                    nodeId, safeBranchKey, cached.resolvedUserId());
            cacheRepository.delete(cached.id());
            return Optional.empty();
        }
    }

    /**
     * Records a newly resolved decision snapshot under the specific condition branch.
     */
    public void recordSuccessfulResolution(
            String definitionId,
            String nodeId,
            String prompt,
            String conditionBranchKey,
            UserProfile initiatorProfile,
            HumanAssigneeResolver.ResolvedAssignee resolved,
            IdentityService identityService
    ) {
        if (definitionId == null || nodeId == null || prompt == null || resolved == null) {
            return;
        }

        String promptHash = computePromptHash(prompt);
        String safeBranchKey = conditionBranchKey != null ? conditionBranchKey : "DEFAULT";
        String initiatorUserId = initiatorProfile != null ? initiatorProfile.userId() : null;
        String initiatorDeptId = initiatorProfile != null ? initiatorProfile.departmentId() : null;
        String initiatorPostCode = initiatorProfile != null ? initiatorProfile.postCode() : null;

        // Capture approver's current organizational snapshot
        String resolvedDeptId = null;
        String resolvedPostCode = null;
        if (resolved.assigneeUser() != null && identityService != null) {
            Optional<UserProfile> resolvedProfileOpt = identityService.getUserProfile(resolved.assigneeUser());
            if (resolvedProfileOpt.isPresent()) {
                resolvedDeptId = resolvedProfileOpt.get().departmentId();
                resolvedPostCode = resolvedProfileOpt.get().postCode();
            }
        }

        Instant now = Instant.now();
        ResolutionCache cache = ResolutionCache.builder(UUID.randomUUID().toString())
                .definitionId(definitionId)
                .nodeId(nodeId)
                .promptHash(promptHash)
                .conditionBranchKey(safeBranchKey)
                .initiatorUserId(initiatorUserId)
                .initiatorDeptId(initiatorDeptId)
                .initiatorPostCode(initiatorPostCode)
                .resolvedUserId(resolved.assigneeUser())
                .resolvedDeptId(resolvedDeptId)
                .resolvedPostCode(resolvedPostCode)
                .resolvedRole(resolved.assigneeRole())
                .candidateUsers(resolved.candidateUsers())
                .candidateRoles(resolved.candidateRoles())
                .hitCount(0)
                .expiresAt(now.plus(defaultTtl))
                .createdAt(now)
                .updatedAt(now)
                .build();

        cacheRepository.save(cache);
        log.info("ResolutionCache RECORDED for Node [{}] Branch [{}] -> Assignee [{}], Dept [{}], TTL [{} days]",
                nodeId, safeBranchKey, resolved.assigneeUser(), resolvedDeptId, defaultTtl.toDays());
    }

    private boolean verifySnapshotAccuracy(
            UserProfile currentInitiator,
            ResolutionCache cached,
            IdentityService identityService
    ) {
        // 1. Check TTL expiry
        if (Instant.now().isAfter(cached.expiresAt())) {
            return false;
        }

        // 2. Check initiator's department and post consistency
        if (currentInitiator != null) {
            if (!Objects.equals(currentInitiator.departmentId(), cached.initiatorDeptId()) ||
                !Objects.equals(currentInitiator.postCode(), cached.initiatorPostCode())) {
                return false;
            }
        }

        // 3. Check approver's existence and organizational status
        if (cached.resolvedUserId() != null && identityService != null) {
            Optional<UserProfile> currentApproverOpt = identityService.getUserProfile(cached.resolvedUserId());
            if (currentApproverOpt.isEmpty()) {
                return false; // Approver no longer exists / has left
            }
            UserProfile currentApprover = currentApproverOpt.get();
            if (cached.resolvedDeptId() != null && !Objects.equals(currentApprover.departmentId(), cached.resolvedDeptId())) {
                return false; // Approver changed department
            }
            if (cached.resolvedPostCode() != null && !Objects.equals(currentApprover.postCode(), cached.resolvedPostCode())) {
                return false; // Approver changed job post
            }
        }

        return true;
    }

    public List<ResolutionCache> getRecentExamples(String definitionId, String nodeId, int limit) {
        if (definitionId == null || nodeId == null) return List.of();
        return cacheRepository.findRecentByDefinitionAndNode(definitionId, nodeId, limit);
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
