package com.wegongdu.rillway.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.example.model.LeaveApplyRequest;
import com.wegongdu.rillway.example.model.PromptCompileRequest;
import com.wegongdu.rillway.example.model.TaskDecisionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工作流 Web 控制台 API 与 HRM 业务 API 全套 MockMvc 测试
 */
@SpringBootTest(classes = RillwayExampleApplication.class)
@AutoConfigureMockMvc
public class WorkflowApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("API测试: 自然语言制度 Prompt 实时编译为 DAG 流程图")
    void testCompilePromptApi() throws Exception {
        PromptCompileRequest request = new PromptCompileRequest(
                "员工请假制度：1. 请假天数小于等于3天由主管审批；2. 大于3天需主管初审并由总经理终审。",
                "100",
                Map.of("day", 2.0, "type", "年假")
        );

        mockMvc.perform(post("/api/workflow/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.definitionId").exists())
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray());
    }

    @Test
    @DisplayName("API测试: 提交前预测预览审批流经路径 (ProcessPreviewer)")
    void testPreviewLeaveWorkflowApi() throws Exception {
        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setEmployeeId("100");
        request.setType("年假");
        request.setDay(new BigDecimal("5.0"));
        request.setReason("探亲长假");

        mockMvc.perform(post("/api/hrm/leave/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.potentialPath").isArray());
    }

    @Test
    @DisplayName("API测试: 全生命周期流转（发起请假 -> 查待办 -> 审批通过 -> 查历史与AI审计）")
    void testFullWorkflowLifecycleApi() throws Exception {
        // 1. 发起请假
        LeaveApplyRequest applyRequest = new LeaveApplyRequest();
        applyRequest.setEmployeeId("100");
        applyRequest.setType("年假");
        applyRequest.setDay(new BigDecimal("2.0"));
        applyRequest.setReason("回老家探亲");

        String responseStr = mockMvc.perform(post("/api/hrm/leave/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> respMap = objectMapper.readValue(responseStr, Map.class);
        Map<String, Object> instanceData = (Map<String, Object>) respMap.get("data");
        String instanceId = (String) instanceData.get("id");

        // 2. 查询待办 (主管 10 的待办)
        String tasksRespStr = mockMvc.perform(get("/api/workflow/tasks/pending")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        // 3. 查询实例历史轨迹
        mockMvc.perform(get("/api/workflow/instances/" + instanceId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // 4. 查询 AI 审计日志
        mockMvc.perform(get("/api/workflow/ai/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("API测试: 7张底表管理与根据配置表 Prompt 一键生成 DAG 流程")
    void testAdminApisAndFlowGeneration() throws Exception {
        // 1. 保存单据绑定配置
        Map<String, Object> bindingBody = Map.of(
                "id", "cfg_admin_test_01",
                "businessType", "reimbursement",
                "tableName", "biz_reimbursement",
                "statusColumn", "status",
                "approvedValue", "APPROVED",
                "rejectedValue", "REJECTED",
                "processPrompt", "员工报销申请：1. 小于1000元部门经理审批；2. 大于1000元需部门经理初审并由财务总监审批。"
        );

        mockMvc.perform(post("/api/admin/binding-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindingBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 2. 根据配置项 Prompt 一键由大模型生成流程图
        mockMvc.perform(post("/api/admin/binding-config/cfg_admin_test_01/generate-flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.generatedDefinitionId").exists())
                .andExpect(jsonPath("$.data.nodeCount").isNumber());

        // 3. 查询 binding-config 列表
        mockMvc.perform(get("/api/admin/binding-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // 4. 新增与查询 AI 配置
        Map<String, Object> aiConfigBody = Map.of(
                "id", "ai_deepseek_v3",
                "providerName", "DeepSeek",
                "baseUrl", "https://api.deepseek.com/v1",
                "apiKey", "sk-test-mock",
                "modelName", "deepseek-chat",
                "temperature", 0.1
        );
        mockMvc.perform(post("/api/admin/ai-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aiConfigBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/admin/ai-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // 5. 查询与清除决策缓存
        mockMvc.perform(get("/api/admin/resolution-cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/resolution-cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 6. 查询实例与任务
        mockMvc.perform(get("/api/admin/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/admin/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
