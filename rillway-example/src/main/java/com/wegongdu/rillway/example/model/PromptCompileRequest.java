package com.wegongdu.rillway.example.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

@Schema(description = "自然语言制度 Prompt 编译请求")
public class PromptCompileRequest implements Serializable {

    @Schema(description = "自然语言审批制度描述文本", example = "员工请假制度：1. 小于等于3天由直属主管审批；2. 大于3天需主管初审并由总经理终审。")
    private String prompt;

    @Schema(description = "发起人用户ID", example = "100")
    private String initiator;

    @Schema(description = "单据业务变量（供大模型评估条件表达式）", example = "{\"day\": 3.5, \"type\": \"年假\"}")
    private Map<String, Object> variables;

    public PromptCompileRequest() {}

    public PromptCompileRequest(String prompt, String initiator, Map<String, Object> variables) {
        this.prompt = prompt;
        this.initiator = initiator;
        this.variables = variables;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}
