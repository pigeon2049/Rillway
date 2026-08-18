package com.wegongdu.rillway.example.hrm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wegongdu.rillway.core.annotation.ProcessInitiator;
import com.wegongdu.rillway.core.annotation.RillwayEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HRM 考勤请假单单据实体 (集成 MyBatis-Plus 与 OpenAPI 注解)
 */
@TableName("hrm_attendance_leave")
@RillwayEntity(businessType = "hrm_attendance_leave")
@Schema(description = "HRM 考勤请假单数据实体")
public class HrmAttendanceLeave implements Serializable {

    /**
     * 请假单主键编号
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "请假单主键ID", example = "10001")
    private Long id;

    /**
     * 发起人员工ID / 用户ID
     */
    @ProcessInitiator
    @TableField("employee_id")
    @Schema(description = "申请人员工ID / 用户ID", example = "100")
    private String employeeId;

    /**
     * 请假类型 (如: 年假, 事假, 病假, 婚假)
     */
    @TableField("type")
    @Schema(description = "请假类型 (年假/事假/病假/婚假)", example = "年假")
    private String type;

    /**
     * 请假天数 (流程条件变量)
     */
    @TableField("\"day\"")
    @Schema(description = "请假天数", example = "2.5")
    private BigDecimal day;

    /**
     * 请假理由
     */
    @TableField("reason")
    @Schema(description = "请假具体事由", example = "回老家探亲")
    private String reason;

    /**
     * 审批状态 (1: 审批中, 2: 审批通过, 3: 审批不通过)
     */
    @TableField("approval_status")
    @Schema(description = "审批状态 (1: 审批中, 2: 审批通过, 3: 审批不通过)", example = "1")
    private Integer approvalStatus;

    /**
     * 请假开始时间
     */
    @TableField("start_time")
    @Schema(description = "请假开始时间")
    private LocalDateTime startTime;

    /**
     * 请假结束时间
     */
    @TableField("end_time")
    @Schema(description = "请假结束时间")
    private LocalDateTime endTime;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注说明")
    private String remark;

    /**
     * 租户编号
     */
    @TableField("tenant_id")
    @Schema(description = "多租户编号", example = "1")
    private Long tenantId;

    public HrmAttendanceLeave() {
    }

    public HrmAttendanceLeave(Long id, String employeeId, String type, BigDecimal day, String reason, Integer approvalStatus) {
        this.id = id;
        this.employeeId = employeeId;
        this.type = type;
        this.day = day;
        this.reason = reason;
        this.approvalStatus = approvalStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String employeeId;
        private String type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal day;
        private String reason;
        private String remark;
        private Integer approvalStatus;
        private Long tenantId;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(String employeeId) { this.employeeId = employeeId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder day(BigDecimal day) { this.day = day; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder approvalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }

        public HrmAttendanceLeave build() {
            HrmAttendanceLeave leave = new HrmAttendanceLeave(id, employeeId, type, day, reason, approvalStatus);
            leave.setStartTime(startTime);
            leave.setEndTime(endTime);
            leave.setRemark(remark);
            leave.setTenantId(tenantId);
            return leave;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getDay() { return day; }
    public void setDay(BigDecimal day) { this.day = day; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
