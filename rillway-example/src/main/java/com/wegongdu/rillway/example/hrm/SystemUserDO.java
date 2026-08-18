package com.wegongdu.rillway.example.hrm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 脱敏后的系统员工/用户实体 (集成 MyBatis-Plus 与 OpenAPI/Swagger 注解)
 */
@TableName("system_users")
@Schema(description = "系统用户及员工档案表")
public class SystemUserDO implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID/员工主键", example = "100")
    private Long id;

    @TableField("username")
    @Schema(description = "用户账号名称", example = "alice_emp")
    private String username;

    @TableField("nickname")
    @Schema(description = "用户真实姓名/昵称", example = "爱丽丝")
    private String nickname;

    @TableField("dept_id")
    @Schema(description = "所属部门ID", example = "101")
    private Long deptId;

    @TableField("direct_leader_id")
    @Schema(description = "直属上级领导用户ID", example = "10")
    private Long directLeaderId;

    @TableField("mobile")
    @Schema(description = "联系手机号码", example = "13800000100")
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
