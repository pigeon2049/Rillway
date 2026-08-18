package com.wegongdu.rillway.core.identity;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.node.HumanNode;

import java.util.List;

/**
 * SPI for resolving natural language assignee prompts into concrete users and roles.
 */
public interface HumanAssigneeResolver {

    record ResolvedAssignee(
            String assigneeUser,
            String assigneeRole,
            List<String> candidateUsers,
            List<String> candidateRoles
    ) {
        public static ResolvedAssignee of(String user, String role, List<String> candidateUsers, List<String> candidateRoles) {
            return new ResolvedAssignee(
                    user,
                    role,
                    candidateUsers != null ? List.copyOf(candidateUsers) : List.of(),
                    candidateRoles != null ? List.copyOf(candidateRoles) : List.of()
            );
        }
    }

    ResolvedAssignee resolve(HumanNode node, ProcessContext context);
}
