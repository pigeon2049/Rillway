package com.wegongdu.rillway.example.hrm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 脱敏后的系统角色实体 (集成 MyBatis-Plus 与 OpenAPI/Swagger 注解)
 */
@TableName("system_role")
@Schema(description = "系统角色权限表")
public class SystemRoleDO implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "角色主键ID", example = "1")
    private Long id;

    @TableField("name")
    @Schema(description = "角色中文名称", example = "总经理")
    private String name;

    @TableField("code")
    @Schema(description = "角色权限编码字符串", example = "ROLE_GENERAL_MANAGER")
    private String code;

    public SystemRoleDO() {}

    public SystemRoleDO(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
