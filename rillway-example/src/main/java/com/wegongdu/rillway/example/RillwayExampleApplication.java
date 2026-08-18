package com.wegongdu.rillway.example;

import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.core.identity.OrgEntityRegistry;
import com.wegongdu.rillway.example.agent.LeaveComplianceAgent;
import com.wegongdu.rillway.example.agent.PurchaseReviewAgent;
import com.wegongdu.rillway.example.hrm.SystemDeptDO;
import com.wegongdu.rillway.example.hrm.SystemPostDO;
import com.wegongdu.rillway.example.hrm.SystemRoleDO;
import com.wegongdu.rillway.example.hrm.SystemUserDO;
import com.wegongdu.rillway.policy.model.PolicyDocument;
import com.wegongdu.rillway.policy.provider.InMemoryPolicyProvider;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rillway AI 原生工作流引擎 Standard Showcase Application
 */
@SpringBootApplication
public class RillwayExampleApplication {

    private static final Logger log = LoggerFactory.getLogger(RillwayExampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RillwayExampleApplication.class, args);
        log.info("""
            ========================================================================================
            🚀 [Rillway Example Service] 启动成功！
            
            🔗 核心展示 API 端点：
              1. 自然语言 Prompt 编译为 DAG:  POST http://localhost:8080/api/workflow/compile
              2. 提交前预测预览审批链路:       POST http://localhost:8080/api/hrm/leave/preview
              3. 业务实体零代码流程发起:       POST http://localhost:8080/api/hrm/leave/apply
              4. 用户待办任务查询与办理:       GET  http://localhost:8080/api/workflow/tasks/pending?userId=10
              5. 大模型调用与 Tool Calling 审计: GET http://localhost:8080/api/workflow/ai/traces
            ========================================================================================
        """);
    }

    /**
     * 1. 注册零代码组织架构实体类（引擎自动反射与 DDL 自省）
     */
    @Bean
    public OrgEntityRegistry orgEntityRegistry() {
        return OrgEntityRegistry.builder()
                .userEntity(SystemUserDO.class)      // 员工/用户实体
                .deptEntity(SystemDeptDO.class)      // 部门实体
                .roleEntity(SystemRoleDO.class)      // 角色实体
                .postEntity(SystemPostDO.class)      // 岗位实体
                .build();
    }

    /**
     * 2. 初始化示例 AI 智能体与自然语言审批制度配置
     */
    @Bean
    public CommandLineRunner initDemoAgentsAndRules(
            AgentRegistry agentRegistry,
            PolicyProvider policyProvider,
            LeaveComplianceAgent leaveAgent,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            // 注册 AI 智能体
            agentRegistry.register(leaveAgent);
            agentRegistry.register(new PurchaseReviewAgent());

            // 注册制度文档
            if (policyProvider instanceof InMemoryPolicyProvider inMemoryProvider) {
                inMemoryProvider.registerDocument(PolicyDocument.of(
                        "HRM_LEAVE_POLICY_2026",
                        "企业考勤与综合请假管理制度(2026版)",
                        "1. 请假天数小于等于 3 天时，由直属部门主管审批；\n" +
                                "2. 请假天数大于 3 天时，需由直属主管初审并升级至总经理终审；\n" +
                                "3. 婚假或产假由人事总监审批；\n" +
                                "4. 事假或病假且天数大于2天需人事经理合规备案。",
                        "HRM_LEAVE_POLICY_2026", "HRM"
                ));
            }

            // 初始化配置表请假制度 Prompt
            try {
                jdbcTemplate.update("DELETE FROM rillway_binding_config WHERE business_type = 'hrm_attendance_leave'");
                jdbcTemplate.update("""
                    INSERT INTO rillway_binding_config (
                        id, business_type, process_definition_id, process_prompt, table_name, primary_key_column, status_column, approved_value, rejected_value, enabled
                    ) VALUES (
                        'cfg_hrm_leave_01',
                        'hrm_attendance_leave',
                        'hrm_attendance_leave',
                        '员工提交请假申请：
                         1. 请假天数小于等于 3 天时，由直属部门主管审批；
                         2. 请假天数大于 3 天时，需由直属部门主管初审，并升级至总经理审批。',
                        'hrm_attendance_leave',
                        'id',
                        'approval_status',
                        '2',
                        '3',
                        true
                    )
                """);
                log.info("✅ [初始化完成] 已自动载入 HRM 考勤请假单绑定规则与 Prompt 制度");
            } catch (Exception e) {
                log.warn("Init binding config fallback: {}", e.getMessage());
            }
        };
    }
}
