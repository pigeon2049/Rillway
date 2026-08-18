package com.wegongdu.rillway.example.hrm;

import java.io.Serializable;

/**
 * 脱敏后的企业部门实体
 */
public class SystemDeptDO implements Serializable {
    private Long id;
    private String name;
    private Long leaderUserId;
    private Long parentId;

    public SystemDeptDO() {}

    public SystemDeptDO(Long id, String name, Long leaderUserId, Long parentId) {
        this.id = id;
        this.name = name;
        this.leaderUserId = leaderUserId;
        this.parentId = parentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getLeaderUserId() { return leaderUserId; }
    public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
