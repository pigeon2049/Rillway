package com.wegongdu.rillway.core.identity;

import java.util.List;
import java.util.Optional;

/**
 * Lightweight SPI for querying enterprise organizational structure, profiles, leaders, departments, and roles.
 */
public interface IdentityService {

    /**
     * Finds the full organizational profile of a specified user (department, post, roles, direct leader, etc.).
     */
    Optional<UserProfile> getUserProfile(String userId);

    /**
     * Finds the direct leader/manager user ID of a specified user.
     */
    Optional<String> getDirectLeader(String userId);

    /**
     * Finds an effective leader, falling back to department manager if direct leader is missing/inactive.
     */
    default Optional<String> getEffectiveDirectLeader(String userId) {
        Optional<String> leader = getDirectLeader(userId);
        if (leader.isPresent()) return leader;

        return getUserProfile(userId)
                .map(UserProfile::departmentId)
                .flatMap(this::getDepartmentManager);
    }

    /**
     * Finds the manager/head user ID of a specified department.
     */
    Optional<String> getDepartmentManager(String departmentId);

    /**
     * Finds all user IDs belonging to a specified post/job position code.
     */
    List<String> getUsersByPost(String postCode);

    /**
     * Finds all user IDs holding a specified role code.
     */
    List<String> getUsersByRole(String roleCode);

    /**
     * Finds all user IDs belonging to a specified department.
     */
    List<String> getUsersByDepartment(String departmentId);
}
