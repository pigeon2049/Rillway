package com.wegongdu.rillway.ai.identity;

import com.wegongdu.rillway.ai.llm.FakeLlmClient;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.node.HumanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI-native human assignee resolver powered by LLMs, UserProfiles, and organizational Tool Calling.
 */
public class AiAssigneeResolver implements HumanAssigneeResolver {

    private final LlmClient llmClient;
    private final IdentityService identityService;

    public AiAssigneeResolver(IdentityService identityService) {
        this(new FakeLlmClient(), identityService);
    }

    public AiAssigneeResolver(LlmClient llmClient, IdentityService identityService) {
        this.llmClient = llmClient != null ? llmClient : new FakeLlmClient();
        this.identityService = identityService;
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
            ResolvedFromPrompt result = resolveWithAiOrSemantic(targetPrompt, context);
            if (result.userId != null) {
                assigneeUser = result.userId;
            }
            if (result.roleCode != null) {
                assigneeRole = result.roleCode;
            }
            if (result.candidateUsers != null && !result.candidateUsers.isEmpty()) {
                candidateUsers.addAll(result.candidateUsers);
            }
        }

        return ResolvedAssignee.of(assigneeUser, assigneeRole, candidateUsers, candidateRoles);
    }

    private record ResolvedFromPrompt(String userId, String roleCode, List<String> candidateUsers) {}

    private ResolvedFromPrompt resolveWithAiOrSemantic(String prompt, ProcessContext context) {
        String initiator = context != null && context.initiator() != null ? context.initiator() : "default_user";
        String lowerPrompt = prompt.toLowerCase();

        // 1. Check if LLM client provides Tool Calling
        List<LlmClient.ToolDefinition> tools = List.of(
                new LlmClient.ToolDefinition("getUserProfile", "Get complete organizational profile of a user (department, post, roles, leader)", Map.of("userId", "string")),
                new LlmClient.ToolDefinition("getDirectLeader", "Get direct leader of a user", Map.of("userId", "string")),
                new LlmClient.ToolDefinition("getDepartmentManager", "Get manager of a department", Map.of("departmentId", "string")),
                new LlmClient.ToolDefinition("getUsersByPost", "Get users belonging to a post/job position", Map.of("postCode", "string")),
                new LlmClient.ToolDefinition("getUsersByRole", "Get users having a role", Map.of("roleCode", "string"))
        );

        LlmClient.LlmResponse response = llmClient.chat(
                "You are an organizational identity resolution agent. Analyze the intent and call the appropriate tools to find the assignee.",
                "Initiator: " + initiator + ", Prompt: " + prompt + ", Context: " + (context != null ? context.variables() : "{}"),
                tools
        );

        if (response.hasToolCalls()) {
            for (LlmClient.ToolCall call : response.toolCalls()) {
                if ("getUserProfile".equals(call.toolName())) {
                    String uid = (String) call.arguments().getOrDefault("userId", initiator);
                    Optional<UserProfile> profileOpt = identityService.getUserProfile(uid);
                    if (profileOpt.isPresent()) {
                        UserProfile profile = profileOpt.get();
                        if (lowerPrompt.contains("主管") || lowerPrompt.contains("负责人") || lowerPrompt.contains("经理")) {
                            return new ResolvedFromPrompt(identityService.getDepartmentManager(profile.departmentId()).orElse(null), null, List.of());
                        }
                        if (profile.directLeaderId() != null) {
                            return new ResolvedFromPrompt(profile.directLeaderId(), null, List.of());
                        }
                    }
                }
                if ("getDirectLeader".equals(call.toolName())) {
                    String uid = (String) call.arguments().getOrDefault("userId", initiator);
                    return new ResolvedFromPrompt(identityService.getDirectLeader(uid).orElse(null), null, List.of());
                }
                if ("getDepartmentManager".equals(call.toolName())) {
                    String deptId = (String) call.arguments().get("departmentId");
                    return new ResolvedFromPrompt(identityService.getDepartmentManager(deptId).orElse(null), null, List.of());
                }
                if ("getUsersByPost".equals(call.toolName())) {
                    String postCode = (String) call.arguments().get("postCode");
                    return new ResolvedFromPrompt(null, null, identityService.getUsersByPost(postCode));
                }
                if ("getUsersByRole".equals(call.toolName())) {
                    String roleCode = (String) call.arguments().get("roleCode");
                    return new ResolvedFromPrompt(null, roleCode, identityService.getUsersByRole(roleCode));
                }
            }
        }

        // 2. High-accuracy semantic understanding & UserProfile penetration
        Optional<UserProfile> initiatorProfileOpt = identityService.getUserProfile(initiator);

        // 2.1 Department head / manager resolution (dept-aware)
        if (lowerPrompt.contains("主管") || lowerPrompt.contains("部门负责人") || lowerPrompt.contains("部门经理") || lowerPrompt.contains("head")) {
            String deptId = null;
            if (initiatorProfileOpt.isPresent()) {
                deptId = initiatorProfileOpt.get().departmentId();
            }
            if (deptId == null && context != null && context.get("department") != null) {
                deptId = context.getString("department");
            }
            if (deptId != null) {
                Optional<String> mgrOpt = identityService.getDepartmentManager(deptId);
                if (mgrOpt.isPresent()) {
                    return new ResolvedFromPrompt(mgrOpt.get(), null, List.of());
                }
            }
        }

        // 2.2 Direct leader resolution
        if (lowerPrompt.contains("领导") || lowerPrompt.contains("上级") || lowerPrompt.contains("leader") || lowerPrompt.contains("manager")) {
            Optional<String> leaderOpt = identityService.getDirectLeader(initiator);
            if (leaderOpt.isPresent()) {
                return new ResolvedFromPrompt(leaderOpt.get(), null, List.of());
            }
        }

        // 2.3 Post / Role resolution
        if (lowerPrompt.contains("岗位") || lowerPrompt.contains("post")) {
            List<String> users = identityService.getUsersByPost(prompt);
            return new ResolvedFromPrompt(null, null, users);
        }

        return new ResolvedFromPrompt(null, null, List.of());
    }

    private boolean isNaturalLanguage(String str) {
        if (str == null || str.isBlank()) return false;
        return str.contains(" ") || str.contains("的") || str.contains("审批") || str.contains("领导") || str.contains("主管") || str.contains("负责人");
    }
}
