package com.wegongdu.rillway.example.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "待办任务审批/驳回请求")
public class TaskDecisionRequest implements Serializable {

    @Schema(description = "操作人用户ID", example = "10")
    private String actorId;

    @Schema(description = "审批意见/批注", example = "同意请假申请")
    private String comment;

    public TaskDecisionRequest() {}

    public TaskDecisionRequest(String actorId, String comment) {
        this.actorId = actorId;
        this.comment = comment;
    }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
