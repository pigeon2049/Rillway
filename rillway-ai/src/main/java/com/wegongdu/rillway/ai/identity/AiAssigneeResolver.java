package com.wegongdu.rillway.ai.identity;

import com.wegongdu.rillway.ai.branch.BranchConditionExtractor;
import com.wegongdu.rillway.ai.cache.InMemoryResolutionCacheRepository;
import com.wegongdu.rillway.ai.cache.ResolutionCacheManager;
import com.wegongdu.rillway.ai.llm.FakeLlmClient;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.node.HumanNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI-native human assignee resolver powered by LLMs, UserProfiles, organizational Tool Calling,
 * and Condition-Branch Isolated ResolutionCache for zero-token execution.
 */
public class AiAssigneeResolver implements HumanAssigneeResolver {

    private static final Logger log = LoggerFactory.getLogger(AiAssigneeResolver.class);

    private final LlmClient llmClient;
    private final IdentityService identityService;
    private final ResolutionCacheManager cacheManager;

    public AiAssigneeResolver(IdentityService identityService) {
        this(new FakeLlmClient(), identityService, new ResolutionCacheManager(new InMemoryResolutionCacheRepository()));
    }

    public AiAssigneeResolver(LlmClient llmClient, IdentityService identityService) {
        this(llmClient, identityService, new ResolutionCacheManager(new InMemoryResolutionCacheRepository()));
    }

    public AiAssigneeResolver(LlmClient llmClient, IdentityService identityService, ResolutionCacheManager cacheManager) {
        this.llmClient = llmClient != null ? llmClient : new FakeLlmClient();
        this.identityService = identityService;
        this.cacheManager = cacheManager != null ? cacheManager : new ResolutionCacheManager(new InMemoryResolutionCacheRepository());
    }

    @Override
    public ResolvedAssignee resolve(HumanNode node, ProcessContext context) {
        if (node == null) {
            return ResolvedAssignee.of(null, null, List.of(), List.of());
        }

        String prompt = node.assigneePrompt();
        String assigneeUser = node.assigneeUser();
        String assigneeRole = node.assigneeRole();
        List<String> candidateUsers = new ArrayList<>(node.candidateUsers());
        List<String> candidateRoles = new ArrayList<>(node.candidateRoles());

        // Check if there is a natural language prompt or user field is natural language
        String targetPrompt = prompt;
        if ((targetPrompt == null || targetPrompt.isBlank()) && isNaturalLanguage(assigneeUser)) {
            targetPrompt = assigneeUser;
            assigneeUser = null;
        }

        if (targetPrompt != null && !targetPrompt.isBlank() && identityService != null) {
            String initiator = context != null && context.initiator() != null ? context.initiator() : "default_user";
            UserProfile initiatorProfile = identityService.getUserProfile(initiator).orElse(null);

            // Compute condition branch fingerprint from context variables (e.g. leaveDays>3 vs leaveDays<=3)
            String branchKey = BranchConditionExtractor.computeBranchKey(targetPrompt, context);

            // 1. Fast-Path: Check valid condition-branch isolated decision cache (0 Token)
            Optional<ResolvedAssignee> cachedOpt = cacheManager.findValidAssignee(
                    node.id(),
                    node.id(),
                    targetPrompt,
                    branchKey,
                    initiatorProfile,
                    identityService
            );

            if (cachedOpt.isPresent()) {
                return cachedOpt.get();
            }

            // 2. Slow-Path: Resolve with standard LLM Tool Calling loop
            ResolvedAssignee newlyResolved = executeLlmToolCallingLoop(targetPrompt, context, initiatorProfile, branchKey);
            if (newlyResolved.assigneeUser() != null) {
                assigneeUser = newlyResolved.assigneeUser();
            }
            if (newlyResolved.assigneeRole() != null) {
                assigneeRole = newlyResolved.assigneeRole();
            }
            if (!newlyResolved.candidateUsers().isEmpty()) {
                candidateUsers.addAll(newlyResolved.candidateUsers());
            }
            if (!newlyResolved.candidateRoles().isEmpty()) {
                candidateRoles.addAll(newlyResolved.candidateRoles());
            }

            // 3. Record successful snapshot into cache isolated by branchKey for future zero-token execution
            ResolvedAssignee finalResult = ResolvedAssignee.of(assigneeUser, assigneeRole, candidateUsers, candidateRoles);
            if (assigneeUser != null || assigneeRole != null || !candidateUsers.isEmpty()) {
                cacheManager.recordSuccessfulResolution(
                        node.id(),
                        node.id(),
                        targetPrompt,
                        branchKey,
                        initiatorProfile,
                        finalResult,
                        identityService
                );
            }

            return finalResult;
        }

        return ResolvedAssignee.of(assigneeUser, assigneeRole, candidateUsers, candidateRoles);
    }

    /**
     * Standard LLM Tool Calling loop.
     */
    private ResolvedAssignee executeLlmToolCallingLoop(
            String prompt,
            ProcessContext context,
            UserProfile initiatorProfile,
            String branchKey
    ) {
        String initiator = initiatorProfile != null ? initiatorProfile.userId() : (context != null ? context.initiator() : "default_user");

        List<LlmClient.ToolDefinition> availableTools = List.of(
                new LlmClient.ToolDefinition("getUserProfile", "Get complete organizational profile of a user (department, post, roles, leader)", Map.of("userId", "string")),
                new LlmClient.ToolDefinition("getDirectLeader", "Get direct leader user ID of a user", Map.of("userId", "string")),
                new LlmClient.ToolDefinition("getDepartmentManager", "Get manager user ID of a department", Map.of("departmentId", "string")),
                new LlmClient.ToolDefinition("getUsersByPost", "Get list of user IDs belonging to a post", Map.of("postCode", "string")),
                new LlmClient.ToolDefinition("getUsersByRole", "Get list of user IDs having a role", Map.of("roleCode", "string")),
                new LlmClient.ToolDefinition("getUsersByDepartment", "Get list of user IDs in a department", Map.of("departmentId", "string"))
        );

        String systemPrompt = """
            You are an AI-Native organizational workflow dispatcher with condition branch understanding.
            Evaluate any conditions in the prompt against context variables (e.g. leaveDays, amount) and use organizational tools to find the right approver.
            """;

        String userPrompt = "Initiator: " + initiator + ", Prompt: " + prompt + ", Variables: " + (context != null ? context.variables() : "{}") + ", Branch: " + branchKey;

        LlmClient.LlmResponse response = llmClient.chat(systemPrompt, userPrompt, availableTools);

        String resolvedUser = null;
        String resolvedRole = null;
        List<String> candidateUsers = new ArrayList<>();

        if (response.hasToolCalls()) {
            List<LlmClient.ToolResult> toolResults = new ArrayList<>();
            for (LlmClient.ToolCall call : response.toolCalls()) {
                Object toolOutput = executeSingleTool(call, initiator);
                toolResults.add(new LlmClient.ToolResult(call.callId(), call.toolName(), toolOutput));

                if (toolOutput instanceof String uid) {
                    resolvedUser = uid;
                } else if (toolOutput instanceof Optional<?> opt && opt.isPresent()) {
                    Object val = opt.get();
                    if (val instanceof String s) {
                        resolvedUser = s;
                    } else if (val instanceof UserProfile up) {
                        if (up.directLeaderId() != null) resolvedUser = up.directLeaderId();
                    }
                } else if (toolOutput instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof String s) candidateUsers.add(s);
                    }
                }
            }

            LlmClient.LlmResponse followUp = llmClient.continueChat(systemPrompt, userPrompt, toolResults);
            if (followUp != null && followUp.content() != null && !followUp.content().isBlank()) {
                if (resolvedUser == null && !followUp.content().contains(" ")) {
                    resolvedUser = followUp.content().trim();
                }
            }
        }

        // Fallback for standalone / offline evaluation
        if (resolvedUser == null && candidateUsers.isEmpty()) {
            if (branchKey.contains(">")) {
                // High-tier condition branch fallback (e.g. GM / CEO / Director)
                Optional<String> gmOpt = identityService.getDepartmentManager("DEPT_GM");
                if (gmOpt.isPresent()) {
                    resolvedUser = gmOpt.get();
                } else {
                    List<String> gmRoleUsers = identityService.getUsersByRole("ROLE_GM");
                    if (!gmRoleUsers.isEmpty()) resolvedUser = gmRoleUsers.get(0);
                }
            }

            if (resolvedUser == null) {
                if (initiatorProfile != null && initiatorProfile.departmentId() != null) {
                    resolvedUser = identityService.getDepartmentManager(initiatorProfile.departmentId()).orElse(null);
                }
                if (resolvedUser == null && identityService != null) {
                    resolvedUser = identityService.getEffectiveDirectLeader(initiator).orElse(null);
                }
            }
        }

        return ResolvedAssignee.of(resolvedUser, resolvedRole, candidateUsers, List.of());
    }

    private Object executeSingleTool(LlmClient.ToolCall call, String defaultInitiator) {
        String toolName = call.toolName();
        Map<String, Object> args = call.arguments() != null ? call.arguments() : Map.of();

        return switch (toolName) {
            case "getUserProfile" -> {
                String uid = (String) args.getOrDefault("userId", defaultInitiator);
                yield identityService.getUserProfile(uid);
            }
            case "getDirectLeader" -> {
                String uid = (String) args.getOrDefault("userId", defaultInitiator);
                yield identityService.getEffectiveDirectLeader(uid);
            }
            case "getDepartmentManager" -> {
                String deptId = (String) args.get("departmentId");
                yield identityService.getDepartmentManager(deptId);
            }
            case "getUsersByPost" -> {
                String postCode = (String) args.get("postCode");
                yield identityService.getUsersByPost(postCode);
            }
            case "getUsersByRole" -> {
                String roleCode = (String) args.get("roleCode");
                yield identityService.getUsersByRole(roleCode);
            }
            case "getUsersByDepartment" -> {
                String deptId = (String) args.get("departmentId");
                yield identityService.getUsersByDepartment(deptId);
            }
            default -> "UNKNOWN_TOOL: " + toolName;
        };
    }

    private boolean isNaturalLanguage(String str) {
        if (str == null || str.isBlank()) return false;
        return str.contains(" ") || str.contains("的") || str.contains("审批") || str.contains("领导") || str.contains("主管") || str.contains("负责人") || str.contains("总经理");
    }
}
