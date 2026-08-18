package com.wegongdu.rillway.runtime.identity;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.node.HumanNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically resolves human assignees, leaders, departments, and posts from expressions.
 */
public class HumanAssigneeResolver {

    private static final Pattern EXPR_PATTERN = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\(([^)]*)\\)\\}");

    private final IdentityService identityService;

    public HumanAssigneeResolver(IdentityService identityService) {
        this.identityService = identityService != null ? identityService : new DefaultIdentityService();
    }

    public record ResolvedAssignee(
            String assigneeUser,
            String assigneeRole,
            List<String> candidateUsers,
            List<String> candidateRoles
    ) {}

    public ResolvedAssignee resolve(HumanNode node, ProcessContext context) {
        if (node == null) {
            return new ResolvedAssignee(null, null, List.of(), List.of());
        }

        String resolvedUser = node.assigneeUser();
        String resolvedRole = node.assigneeRole();
        List<String> resolvedCandidateUsers = new ArrayList<>(node.candidateUsers());
        List<String> resolvedCandidateRoles = new ArrayList<>(node.candidateRoles());

        // 1. Resolve assigneeUser expression
        if (resolvedUser != null && resolvedUser.startsWith("#{")) {
            resolvedUser = evaluateUserExpression(resolvedUser, context);
        }

        // 2. Resolve candidateUsers expressions
        List<String> expandedCandidateUsers = new ArrayList<>();
        for (String candidate : resolvedCandidateUsers) {
            if (candidate.startsWith("#{")) {
                expandedCandidateUsers.addAll(evaluateUserListExpression(candidate, context));
            } else {
                expandedCandidateUsers.add(candidate);
            }
        }

        return new ResolvedAssignee(
                resolvedUser,
                resolvedRole,
                Collections.unmodifiableList(expandedCandidateUsers),
                Collections.unmodifiableList(resolvedCandidateRoles)
        );
    }

    private String evaluateUserExpression(String expr, ProcessContext context) {
        Matcher matcher = EXPR_PATTERN.matcher(expr.trim());
        if (!matcher.matches()) {
            return expr;
        }

        String function = matcher.group(1);
        String param = cleanParam(matcher.group(2), context);

        switch (function) {
            case "leader":
            case "directLeader":
                return identityService.getDirectLeader(param).orElse(null);
            case "deptManager":
            case "departmentManager":
                return identityService.getDepartmentManager(param).orElse(null);
            default:
                return expr;
        }
    }

    private List<String> evaluateUserListExpression(String expr, ProcessContext context) {
        Matcher matcher = EXPR_PATTERN.matcher(expr.trim());
        if (!matcher.matches()) {
            return List.of(expr);
        }

        String function = matcher.group(1);
        String param = cleanParam(matcher.group(2), context);

        switch (function) {
            case "post":
            case "postUsers":
                return identityService.getUsersByPost(param);
            case "role":
            case "roleUsers":
                return identityService.getUsersByRole(param);
            case "dept":
            case "departmentUsers":
                return identityService.getUsersByDepartment(param);
            case "leader":
                return identityService.getDirectLeader(param).map(List::of).orElse(List.of());
            case "deptManager":
                return identityService.getDepartmentManager(param).map(List::of).orElse(List.of());
            default:
                return List.of(expr);
        }
    }

    private String cleanParam(String rawParam, ProcessContext context) {
        if (rawParam == null || rawParam.isBlank()) {
            return context != null ? context.initiator() : "";
        }
        String trimmed = rawParam.trim();
        if (trimmed.equals("initiator") && context != null) {
            return context.initiator() != null ? context.initiator() : "";
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (context != null && context.get(trimmed) != null) {
            return context.getString(trimmed);
        }
        return trimmed;
    }
}
