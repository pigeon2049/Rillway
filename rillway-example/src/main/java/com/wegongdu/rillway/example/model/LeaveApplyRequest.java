package com.wegongdu.rillway.example.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "请假申请提交请求")
public class LeaveApplyRequest implements Serializable {

    @Schema(description = "申请人员工ID", example = "100")
    private String employeeId;

    @Schema(description = "请假类型 (年假/事假/病假/婚假)", example = "年假")
    private String type;

    @Schema(description = "请假天数", example = "2.5")
    private BigDecimal day;

    @Schema(description = "请假事由", example = "探亲休假")
    private String reason;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    public LeaveApplyRequest() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getDay() { return day; }
    public void setDay(BigDecimal day) { this.day = day; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
