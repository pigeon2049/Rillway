package com.wegongdu.rillway.example;

import com.wegongdu.rillway.agent.spi.Agent;
import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.ai.intent.LlmIntentInterpreter;
import com.wegongdu.rillway.ai.intent.ProcessIntent;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.EntityClassIntrospector;
import com.wegongdu.rillway.core.identity.OrgEntityRegistry;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.DecisionType;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.example.hrm.HrmAttendanceLeave;
import com.wegongdu.rillway.example.hrm.SystemDeptDO;
import com.wegongdu.rillway.example.hrm.SystemRoleDO;
import com.wegongdu.rillway.example.hrm.SystemUserDO;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.preview.PreviewContext;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HRM 考勤请假与企业组织架构多级审批全场景集成测试
 */
@SpringBootTest(classes = PurchaseApplication.class)
public class HrmLeaveWorkflowIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HrmLeaveWorkflowIntegrationTest.class);

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProcessPreviewer processPreviewer;

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private IntentInterpreter intentInterpreter;

    @Autowired
    private LlmClient llmClient;

    @BeforeEach
    void setupHrmSchemaAndConfig() {
        // 1. 初始化脱敏后的业务表结构
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS hrm_attendance_leave (
                id BIGINT NOT NULL PRIMARY KEY,
                employee_id VARCHAR(64) NOT NULL,
                type VARCHAR(32) NOT NULL,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                "day" DECIMAL(5, 1) NOT NULL,
                reason VARCHAR(500),
                remark VARCHAR(500),
                approval_status INT DEFAULT 1,
                creator VARCHAR(64),
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updater VARCHAR(64),
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                deleted INT DEFAULT 0,
                tenant_id BIGINT DEFAULT 1
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS system_users (
                id BIGINT NOT NULL PRIMARY KEY,
                username VARCHAR(64) NOT NULL,
                nickname VARCHAR(64) NOT NULL,
                dept_id BIGINT,
                direct_leader_id BIGINT,
                mobile VARCHAR(32),
                status INT DEFAULT 0,
                deleted INT DEFAULT 0
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS system_dept (
                id BIGINT NOT NULL PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                leader_user_id BIGINT,
                parent_id BIGINT DEFAULT 0,
                status INT DEFAULT 0,
                deleted INT DEFAULT 0
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS system_role (
                id BIGINT NOT NULL PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                code VARCHAR(64) NOT NULL,
                status INT DEFAULT 0,
                deleted INT DEFAULT 0
            )
        """);

        // 2. 初始化脱敏数据
        jdbcTemplate.update("DELETE FROM system_dept");
        jdbcTemplate.update("INSERT INTO system_dept (id, name, leader_user_id, parent_id) VALUES (101, '研发部', 10, 100)");
        jdbcTemplate.update("INSERT INTO system_dept (id, name, leader_user_id, parent_id) VALUES (102, '市场营销部', 20, 100)");

        jdbcTemplate.update("DELETE FROM system_users");
        jdbcTemplate.update("INSERT INTO system_users (id, username, nickname, dept_id, direct_leader_id, mobile) VALUES (1, 'david_gm', '大卫 (总经理)', 100, NULL, '13800000001')");
        jdbcTemplate.update("INSERT INTO system_users (id, username, nickname, dept_id, direct_leader_id, mobile) VALUES (10, 'bob_rd_mgr', '鲍勃 (研发主管)', 101, 1, '13800000010')");
        jdbcTemplate.update("INSERT INTO system_users (id, username, nickname, dept_id, direct_leader_id, mobile) VALUES (100, 'alice_emp', '爱丽丝 (研发工程师)', 101, 10, '13800000100')");

        jdbcTemplate.update("DELETE FROM system_role");
        jdbcTemplate.update("INSERT INTO system_role (id, name, code) VALUES (1, '总经理', 'ROLE_GENERAL_MANAGER')");
        jdbcTemplate.update("INSERT INTO system_role (id, name, code) VALUES (2, '市场总监', 'ROLE_MARKETING_DIRECTOR')");

        // 3. 配置 HRM 考勤请假单绑定规则
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
    }

    @Test
    @DisplayName("测试 HRM 多级审批流转（5天长假：主管初审通过 -> 总经理终审批准 -> 自动回写 approval_status=2）")
    void testMultiLevelLeaveApproval() {
        ProcessDefinition multiLevelDef = ProcessDefinition.builder("hrm_leave_multi_level")
                .name("HRM 长假多级审批流程")
                .version("1.0.0")
                .startNode("start", "流程发起")
                .humanNode("level_1_leader", b -> b.name("直属主管初审").assigneeUser("10"))
                .humanNode("level_2_gm", b -> b.name("总经理终审").assigneeUser("1"))
                .endNode("end", "审批归档")
                .edge("start", "level_1_leader")
                .edge("level_1_leader", "level_2_gm")
                .edge("level_2_gm", "end")
                .build();

        Long testLeaveId = System.currentTimeMillis();
        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(testLeaveId)
                .employeeId("100")
                .type("年假")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(5))
                .day(new BigDecimal("5.0"))
                .reason("长假休养，申请请假5天")
                .approvalStatus(1)
                .tenantId(1L)
                .build();

        // 1. 插入业务底表
        jdbcTemplate.update("""
            INSERT INTO hrm_attendance_leave (id, employee_id, type, "day", reason, approval_status, tenant_id)
            VALUES (?, ?, ?, ?, ?, 1, 1)
        """, testLeaveId, "100", "年假", new BigDecimal("5.0"), "长假休养，申请请假5天");

        // 2. 启动流程
        ProcessInstance instance = processEngine.start(multiLevelDef, leaveDO);
        assertNotNull(instance);
        assertEquals(ProcessStatus.WAITING_FOR_DECISION, instance.status());

        // 3. 第 1 级初审
        List<Task> level1Tasks = taskService.findTasksByProcessInstanceId(instance.id());
        assertEquals(1, level1Tasks.size());
        Task task1 = level1Tasks.get(0);
        assertEquals("level_1_leader", task1.nodeId());

        ProcessInstance afterLevel1 = taskService.completeTask(task1.id(), ApproveDecision.of(Actor.HumanActor.of("10"), "主管同意初审"));
        assertEquals(ProcessStatus.WAITING_FOR_DECISION, afterLevel1.status());

        // 4. 第 2 级终审
        List<Task> level2Tasks = taskService.findTasksByProcessInstanceId(instance.id()).stream()
                .filter(t -> t.status() == TaskStatus.PENDING)
                .toList();
        assertEquals(1, level2Tasks.size());
        Task task2 = level2Tasks.get(0);
        assertEquals("level_2_gm", task2.nodeId());

        ProcessInstance finalInstance = taskService.completeTask(task2.id(), ApproveDecision.of(Actor.HumanActor.of("1"), "总经理批准"));
        assertEquals(ProcessStatus.COMPLETED, finalInstance.status());

        // 5. 校验业务底表状态自动更新为 2
        Integer finalStatus = jdbcTemplate.queryForObject(
                "SELECT approval_status FROM hrm_attendance_leave WHERE id = ?", Integer.class, testLeaveId);
        assertEquals(2, finalStatus, "终审完成后，业务单据 approval_status 应自动更新为 2 (通过)");
    }

    @Test
    @DisplayName("测试审批流程预览（ProcessPreviewer：提交前预知审批路径）")
    void testProcessPreview() {
        ProcessDefinition leaveDef = ProcessDefinition.builder("hrm_leave_preview_demo")
                .name("HRM 请假审批流程（含预览）")
                .startNode("start", "发起请假")
                .humanNode("dept_leader", b -> b.name("直属主管审批").assigneeUser("10"))
                .humanNode("gm", b -> b.name("总经理终审").assigneeUser("1"))
                .endNode("end", "流程结束")
                .edge("start", "dept_leader")
                .edge("dept_leader", "gm")
                .edge("gm", "end")
                .build();

        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(888888L)
                .employeeId("100")
                .type("年假")
                .day(new BigDecimal("5.0"))
                .reason("探亲休假")
                .build();

        var previewResult = processPreviewer.preview(leaveDef, PreviewContext.of("100", ProcessContext.from(leaveDO)));
        assertNotNull(previewResult);
        assertThat(previewResult.potentialPath()).containsExactly("start", "dept_leader", "gm", "end");
    }

    @Test
    @DisplayName("测试 AI 智能审查节点（AgentNode：AI 自动初审合规性 -> 主管终审）")
    void testAiAgentNodeApproval() {
        agentRegistry.register(new Agent() {
            @Override
            public String id() {
                return "hrm_leave_compliance_agent";
            }

            @Override
            public com.wegongdu.rillway.agent.model.AgentDecision decide(com.wegongdu.rillway.agent.model.AgentContext agentContext) {
                return com.wegongdu.rillway.agent.model.AgentDecision.of(
                        ApproveDecision.of(Actor.AgentActor.of("hrm_leave_compliance_agent"), "AI 合规初审通过")
                );
            }
        });

        ProcessDefinition aiDef = ProcessDefinition.builder("hrm_leave_ai_flow")
                .name("HRM AI 智能初审流程")
                .startNode("start", "提交")
                .agentNode("ai_check", b -> b.name("AI合规初审")
                        .agentId("hrm_leave_compliance_agent")
                        .allowedDecisions(DecisionType.APPROVE))
                .humanNode("dept_leader", b -> b.name("部门主管审批").assigneeUser("10"))
                .endNode("end", "归档")
                .edge("start", "ai_check")
                .edge("ai_check", "dept_leader")
                .edge("dept_leader", "end")
                .build();

        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(System.currentTimeMillis())
                .employeeId("100")
                .type("事假")
                .day(new BigDecimal("1.0"))
                .reason("家中急事")
                .build();

        ProcessInstance instance = processEngine.start(aiDef, leaveDO);
        assertNotNull(instance);
        // AI 节点自动秒级执行，流程停在 dept_leader
        assertEquals("dept_leader", instance.currentNodeId());

        List<Task> tasks = taskService.findTasksByProcessInstanceId(instance.id());
        assertEquals(1, tasks.size());
        ProcessInstance finalInstance = taskService.completeTask(tasks.get(0).id(), ApproveDecision.of(Actor.HumanActor.of("10"), "主管同意"));
        assertEquals(ProcessStatus.COMPLETED, finalInstance.status());
    }

    @Test
    @DisplayName("测试从配置表自然语言制度（process_prompt）自动编译生成流程 DAG 并执行")
    void testProcessGenerationFromBindingConfigPrompt() {
        String processPrompt = jdbcTemplate.queryForObject(
                "SELECT process_prompt FROM rillway_binding_config WHERE business_type = 'hrm_attendance_leave' LIMIT 1",
                String.class
        );
        assertNotNull(processPrompt);

        ProcessDefinition definition = intentInterpreter.interpret(ProcessIntent.of(processPrompt));
        assertNotNull(definition);
        assertNotNull(definition.getStartNode());
        assertFalse(definition.edges().isEmpty());

        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(System.currentTimeMillis())
                .employeeId("100")
                .type("年假")
                .day(new BigDecimal("2.0"))
                .reason("探亲休假")
                .build();

        ProcessInstance instance = processEngine.start(definition, leaveDO);
        assertNotNull(instance);
        assertEquals(ProcessStatus.WAITING_FOR_DECISION, instance.status());
    }

    @Test
    @DisplayName("测试大模型感知实体 Schema 并编译多条件分支复合制度")
    void testComplexMultiConditionPolicyCompilation() {
        String complexPolicyPrompt = """
                企业多条件请假审批制度：
                1. 婚假或产假：直接由人事总监审批；
                2. 事假或病假且请假天数大于 2 天：需由直属部门主管初审，并由人事经理终审；
                3. 常规年假且请假天数小于等于 3 天：由直属部门主管直接批准即可；
                4. 其余大于 3 天的各类假期：由直属部门主管初审，并升级至总经理终审。
                """;

        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(System.currentTimeMillis())
                .employeeId("100")
                .type("事假")
                .day(new BigDecimal("3.0"))
                .reason("家中急事处理")
                .build();

        ProcessDefinition complexDag = intentInterpreter.interpret(ProcessIntent.of(
                complexPolicyPrompt,
                "100",
                ProcessContext.from(leaveDO)
        ));

        assertNotNull(complexDag);
        ProcessInstance instance = processEngine.start(complexDag, leaveDO);
        assertNotNull(instance);
        assertEquals(ProcessStatus.WAITING_FOR_DECISION, instance.status());
    }

    @Test
    @DisplayName("测试开发者仅注册 Entity 实体类 Class，引擎全自动反射自省 DDL Schema 并驱动大模型编译与流转")
    void testDeclarativeEntityClassSchemaAwareness() {
        // 1. 注册脱敏后的实体 Class
        OrgEntityRegistry orgRegistry = OrgEntityRegistry.builder()
                .userEntity(SystemUserDO.class)
                .deptEntity(SystemDeptDO.class)
                .roleEntity(SystemRoleDO.class)
                .build();

        // 2. 自省解析校验
        var userMeta = EntityClassIntrospector.introspect(SystemUserDO.class);
        assertNotNull(userMeta);
        assertEquals("system_user", userMeta.tableName());

        // 3. 构建带实体类自省的 LlmIntentInterpreter
        LlmIntentInterpreter autoInterpreter = new LlmIntentInterpreter(llmClient, orgRegistry);

        String orgPolicyPrompt = """
                企业多部门差异化请假制度：
                1. 研发部员工请假天数小于等于 3 天：由直属部门负责人审批；
                2. 市场部员工请假：由市场总监（ROLE_MARKETING_DIRECTOR）审批；
                3. 其余情况或超过 3 天：升级至总经理（ROLE_GENERAL_MANAGER）终审。
                """;

        HrmAttendanceLeave leaveDO = HrmAttendanceLeave.builder()
                .id(System.currentTimeMillis())
                .employeeId("100")
                .type("年假")
                .day(new BigDecimal("2.5"))
                .reason("家庭出游")
                .build();

        ProcessDefinition orgDag = autoInterpreter.interpret(ProcessIntent.of(
                orgPolicyPrompt,
                "100",
                ProcessContext.from(leaveDO)
        ));

        assertNotNull(orgDag);
        ProcessInstance instance = processEngine.start(orgDag, leaveDO);
        assertNotNull(instance);
        assertEquals(ProcessStatus.WAITING_FOR_DECISION, instance.status());
    }
}
