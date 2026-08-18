package com.wegongdu.rillway.example.hrm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 脱敏后的企业部门实体 (集成 MyBatis-Plus 与 OpenAPI/Swagger 注解)
 */
@TableName("system_dept")
@Schema(description = "企业组织架构部门表")
public class SystemDeptDO implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "部门主键ID", example = "101")
    private Long id;

    @TableField("name")
    @Schema(description = "部门全称", example = "研发部")
    private String name;

    @TableField("leader_user_id")
    @Schema(description = "部门负责人/主管用户ID", example = "10")
    private Long leaderUserId;

    @TableField("parent_id")
    @Schema(description = "上级父部门ID", example = "100")
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
