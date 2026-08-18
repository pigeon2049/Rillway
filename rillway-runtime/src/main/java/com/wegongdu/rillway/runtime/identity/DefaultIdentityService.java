package com.wegongdu.rillway.runtime.identity;

import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of IdentityService with UserProfile support.
 */
public class DefaultIdentityService implements IdentityService {

    private final Map<String, UserProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, String> directLeaders = new ConcurrentHashMap<>();
    private final Map<String, String> departmentManagers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> postUsers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> roleUsers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> departmentUsers = new ConcurrentHashMap<>();

    public DefaultIdentityService registerUserProfile(UserProfile profile) {
        if (profile != null && profile.userId() != null) {
            userProfiles.put(profile.userId(), profile);
            if (profile.directLeaderId() != null) {
                directLeaders.putIfAbsent(profile.userId(), profile.directLeaderId());
            }
            if (profile.departmentId() != null) {
                departmentUsers.computeIfAbsent(profile.departmentId(), k -> new ArrayList<>()).add(profile.userId());
            }
            if (profile.postCode() != null) {
                postUsers.computeIfAbsent(profile.postCode(), k -> new ArrayList<>()).add(profile.userId());
            }
            if (profile.roles() != null) {
                for (String role : profile.roles()) {
                    roleUsers.computeIfAbsent(role, k -> new ArrayList<>()).add(profile.userId());
                }
            }
        }
        return this;
    }

    public DefaultIdentityService registerDirectLeader(String userId, String leaderUserId) {
        if (userId != null && leaderUserId != null) {
            directLeaders.put(userId, leaderUserId);
        }
        return this;
    }

    public DefaultIdentityService registerDepartmentManager(String departmentId, String managerUserId) {
        if (departmentId != null && managerUserId != null) {
            departmentManagers.put(departmentId, managerUserId);
        }
        return this;
    }

    public DefaultIdentityService registerPostUser(String postCode, String userId) {
        if (postCode != null && userId != null) {
            postUsers.computeIfAbsent(postCode, k -> new ArrayList<>()).add(userId);
        }
        return this;
    }

    public DefaultIdentityService registerRoleUser(String roleCode, String userId) {
        if (roleCode != null && userId != null) {
            roleUsers.computeIfAbsent(roleCode, k -> new ArrayList<>()).add(userId);
        }
        return this;
    }

    public DefaultIdentityService registerDepartmentUser(String departmentId, String userId) {
        if (departmentId != null && userId != null) {
            departmentUsers.computeIfAbsent(departmentId, k -> new ArrayList<>()).add(userId);
        }
        return this;
    }

    @Override
    public Optional<UserProfile> getUserProfile(String userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(userProfiles.get(userId));
    }

    @Override
    public Optional<String> getDirectLeader(String userId) {
        if (userId == null) return Optional.empty();
        String leader = directLeaders.get(userId);
        if (leader == null && userProfiles.containsKey(userId)) {
            leader = userProfiles.get(userId).directLeaderId();
        }
        return Optional.ofNullable(leader);
    }

    @Override
    public Optional<String> getDepartmentManager(String departmentId) {
        if (departmentId == null) return Optional.empty();
        return Optional.ofNullable(departmentManagers.get(departmentId));
    }

    @Override
    public List<String> getUsersByPost(String postCode) {
        if (postCode == null) return List.of();
        return List.copyOf(postUsers.getOrDefault(postCode, Collections.emptyList()));
    }

    @Override
    public List<String> getUsersByRole(String roleCode) {
        if (roleCode == null) return List.of();
        return List.copyOf(roleUsers.getOrDefault(roleCode, Collections.emptyList()));
    }

    @Override
    public List<String> getUsersByDepartment(String departmentId) {
        if (departmentId == null) return List.of();
        return List.copyOf(departmentUsers.getOrDefault(departmentId, Collections.emptyList()));
    }
}
