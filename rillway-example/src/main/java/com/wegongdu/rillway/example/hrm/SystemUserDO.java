package com.wegongdu.rillway.example.hrm;

import java.io.Serializable;

/**
 * 脱敏后的系统员工/用户实体
 */
public class SystemUserDO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private Long deptId;
    private Long directLeaderId;
    private String mobile;

    public SystemUserDO() {}

    public SystemUserDO(Long id, String username, String nickname, Long deptId, Long directLeaderId, String mobile) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.deptId = deptId;
        this.directLeaderId = directLeaderId;
        this.mobile = mobile;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getDirectLeaderId() { return directLeaderId; }
    public void setDirectLeaderId(Long directLeaderId) { this.directLeaderId = directLeaderId; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
