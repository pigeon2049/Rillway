package com.wegongdu.rillway.example.hrm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 脱敏后的岗位实体 (集成 MyBatis-Plus 与 OpenAPI/Swagger 注解)
 */
@TableName("system_post")
@Schema(description = "企业岗位职务表")
public class SystemPostDO implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "岗位主键ID", example = "1")
    private Long id;

    @TableField("name")
    @Schema(description = "岗位名称", example = "高级研发工程师")
    private String name;

    @TableField("code")
    @Schema(description = "岗位唯一编码", example = "POST_SR_DEV")
    private String code;

    public SystemPostDO() {}

    public SystemPostDO(Long id, String name, String code) {
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
