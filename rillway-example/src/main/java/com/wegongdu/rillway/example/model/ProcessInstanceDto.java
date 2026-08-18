package com.wegongdu.rillway.example.model;

import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;

@Schema(description = "流程实例响应 DTO")
public class ProcessInstanceDto implements Serializable {

    @Schema(description = "流程实例唯一标识 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "关联业务单据 Key", example = "hrm_attendance_leave:17240001")
    private String businessKey;

    @Schema(description = "所绑定的流程定义 ID", example = "hrm_attendance_leave")
    private String definitionId;

    @Schema(description = "当前流程状态 (RUNNING / WAITING_FOR_DECISION / COMPLETED / REJECTED)", example = "WAITING_FOR_DECISION")
    private ProcessStatus status;

    @Schema(description = "当前停留在的节点 ID", example = "dept_leader")
    private String currentNodeId;

    @Schema(description = "流程启动时间")
    private Instant startedAt;

    @Schema(description = "流程完成时间")
    private Instant completedAt;

    public ProcessInstanceDto() {}

    public static ProcessInstanceDto from(ProcessInstance instance) {
        if (instance == null) return null;
        ProcessInstanceDto dto = new ProcessInstanceDto();
        dto.setId(instance.id());
        dto.setBusinessKey(instance.businessKey());
        dto.setDefinitionId(instance.definitionId());
        dto.setStatus(instance.status());
        dto.setCurrentNodeId(instance.currentNodeId());
        dto.setStartedAt(instance.startedAt());
        dto.setCompletedAt(instance.completedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }

    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
