package com.wegongdu.rillway.example;

import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.annotation.RillwayEntity;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.BindingConfig;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_bean_start_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class BeanEntityStartIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BindingConfigRepository bindingConfigRepository;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS biz_purchase_order (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "title VARCHAR(128), " +
                "amount DECIMAL(10,2), " +
                "status VARCHAR(32)" +
                ")");

        // Binding for purchase_order
        bindingConfigRepository.save(BindingConfig.ofPrompt(
                "cfg_bean_01",
                "purchase_order",
                "采购审批：5000元以下直属经理审批；5000元以上AI采购合规审核",
                "biz_purchase_order",
                "status",
                "APPROVED",
                "REJECTED"
        ));

        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            defaultIdentity.registerDirectLeader("Alice", "Manager_Bob");
        }
    }

    // 1. 普通 JavaBean 实体 (带 getter/setter)
    public static class PurchaseOrderBean {
        private String id;
        private String creator;
        private BigDecimal amount;
        private String title;

        public PurchaseOrderBean(String id, String creator, BigDecimal amount, String title) {
            this.id = id;
            this.creator = creator;
            this.amount = amount;
            this.title = title;
        }

        public String getId() { return id; }
        public String getCreator() { return creator; }
        public BigDecimal getAmount() { return amount; }
        public String getTitle() { return title; }
    }

    // 2. Java 21 Record 实体 (使用 @RillwayEntity 显式指定或默认按类名蛇形)
    @RillwayEntity(businessType = "purchase_order")
    public record PurchaseRecord(
            String id,
            String applicant,
            BigDecimal amount,
            String title
    ) {}

    @Test
    @DisplayName("should start workflow directly by passing a standard JavaBean instance")
    void should_start_workflow_by_passing_java_bean() {
        String orderId = "PO_BEAN_001";
        jdbcTemplate.update("INSERT INTO biz_purchase_order (id, title, amount, status) VALUES (?, ?, ?, ?)",
                orderId, "采购服务器", new BigDecimal("3500"), "DRAFT");

        PurchaseOrderBean bean = new PurchaseOrderBean(orderId, "Alice", new BigDecimal("3500"), "采购服务器");

        // 🚀 核心测试：直接将 Bean 传入 processEngine.start(bean)，一行代码发起审批！
        ProcessInstance instance = processEngine.start(bean);

        assertThat(instance).isNotNull();
        assertThat(instance.status()).isEqualTo(ProcessStatus.WAITING_FOR_DECISION);
        assertThat(instance.context().initiator()).isEqualTo("Alice");
        assertThat(instance.context().getDecimal("amount")).isEqualByComparingTo("3500");

        // 验证待办任务自动生成
        List<Task> tasks = taskService.findTasksByBusinessKey("purchase_order:" + orderId);
        assertThat(tasks).isNotEmpty();

        // 经理审批
        taskService.completeTask(tasks.get(0).id(), ApproveDecision.of(Actor.HumanActor.of("Manager_Bob"), "OK"));

        // 验证自动状态回写
        String status = jdbcTemplate.queryForObject("SELECT status FROM biz_purchase_order WHERE id = ?", String.class, orderId);
        assertThat(status).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should start workflow directly by passing a Java 21 Record instance")
    void should_start_workflow_by_passing_record() {
        String orderId = "PO_RECORD_002";
        jdbcTemplate.update("INSERT INTO biz_purchase_order (id, title, amount, status) VALUES (?, ?, ?, ?)",
                orderId, "采购办公用品", new BigDecimal("1200"), "DRAFT");

        PurchaseRecord record = new PurchaseRecord(orderId, "Alice", new BigDecimal("1200"), "采购办公用品");

        // 🚀 核心测试：直接将 Record 传入 processEngine.start(record)！
        ProcessInstance instance = processEngine.start(record);

        assertThat(instance).isNotNull();
        assertThat(instance.context().initiator()).isEqualTo("Alice");
        assertThat(instance.context().getDecimal("amount")).isEqualByComparingTo("1200");

        List<Task> tasks = taskService.findTasksByBusinessKey("purchase_order:" + orderId);
        assertThat(tasks).isNotEmpty();

        taskService.completeTask(tasks.get(0).id(), ApproveDecision.of(Actor.HumanActor.of("Manager_Bob"), "同意"));

        String status = jdbcTemplate.queryForObject("SELECT status FROM biz_purchase_order WHERE id = ?", String.class, orderId);
        assertThat(status).isEqualTo("APPROVED");
    }

    // 3. 带 @ProcessIgnore 和 @ProcessVariable 及敏感字段的测试实体
    @RillwayEntity(businessType = "purchase_order")
    public static class SecurePurchaseOrder {
        private String id;
        private String creator;
        private BigDecimal amount;

        @com.wegongdu.rillway.core.annotation.ProcessVariable("orderTitle")
        private String title;

        @com.wegongdu.rillway.core.annotation.ProcessIgnore
        private String internalPassword;

        private String secretToken; // 敏感名字，默认自动脱敏跳过

        private byte[] largeBinaryData; // 二进制流，自动跳过

        public SecurePurchaseOrder(String id, String creator, BigDecimal amount, String title, String internalPassword, String secretToken, byte[] largeBinaryData) {
            this.id = id;
            this.creator = creator;
            this.amount = amount;
            this.title = title;
            this.internalPassword = internalPassword;
            this.secretToken = secretToken;
            this.largeBinaryData = largeBinaryData;
        }

        public String getId() { return id; }
        public String getCreator() { return creator; }
        public BigDecimal getAmount() { return amount; }
        public String getTitle() { return title; }
        public String getInternalPassword() { return internalPassword; }
        public String getSecretToken() { return secretToken; }
        public byte[] getLargeBinaryData() { return largeBinaryData; }
    }

    @Test
    @DisplayName("should skip @ProcessIgnore and sensitive fields from workflow context")
    void should_filter_ignored_and_sensitive_fields() {
        String orderId = "PO_SECURE_003";
        jdbcTemplate.update("INSERT INTO biz_purchase_order (id, title, amount, status) VALUES (?, ?, ?, ?)",
                orderId, "高密服务器", new BigDecimal("4000"), "DRAFT");

        SecurePurchaseOrder order = new SecurePurchaseOrder(
                orderId, "Alice", new BigDecimal("4000"), "高密服务器",
                "pwd_123456", "sk-token-xxx", new byte[]{1, 2, 3}
        );

        ProcessInstance instance = processEngine.start(order);

        assertThat(instance).isNotNull();
        // 验证正常变量与别名
        assertThat(instance.context().getDecimal("amount")).isEqualByComparingTo("4000");
        assertThat(instance.context().getString("orderTitle")).isEqualTo("高密服务器");

        // 验证敏感/忽略字段绝未进入流程上下文和大模型
        assertThat(instance.context().variables()).doesNotContainKey("internalPassword");
        assertThat(instance.context().variables()).doesNotContainKey("secretToken");
        assertThat(instance.context().variables()).doesNotContainKey("largeBinaryData");
    }

    // 4. 使用 Long 主键与 Long 发起人 ID 的企业级实体 (如 MyBatis-Plus 实体)
    @RillwayEntity(businessType = "purchase_order")
    public static class SnowflakePurchaseOrder {
        private Long id;
        private Long creatorUserId;
        private BigDecimal amount;
        private String title;

        public SnowflakePurchaseOrder(Long id, Long creatorUserId, BigDecimal amount, String title) {
            this.id = id;
            this.creatorUserId = creatorUserId;
            this.amount = amount;
            this.title = title;
        }

        public Long getId() { return id; }
        public Long getCreatorUserId() { return creatorUserId; }
        public BigDecimal getAmount() { return amount; }
        public String getTitle() { return title; }
    }

    @Test
    @DisplayName("should support Long snowflake id and Long initiator userId seamlessly")
    void should_support_long_snowflake_id_and_initiator() {
        Long orderId = 18247291823791283L;
        Long userId = 1001L;

        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            defaultIdentity.registerDirectLeader("1001", "Manager_Bob");
        }

        jdbcTemplate.update("INSERT INTO biz_purchase_order (id, title, amount, status) VALUES (?, ?, ?, ?)",
                String.valueOf(orderId), "雪花ID采购单", new BigDecimal("2800"), "DRAFT");

        SnowflakePurchaseOrder order = new SnowflakePurchaseOrder(orderId, userId, new BigDecimal("2800"), "雪花ID采购单");

        // 一行代码发起
        ProcessInstance instance = processEngine.start(order);

        assertThat(instance).isNotNull();
        // 验证 String 格式与 Long 便捷方法
        assertThat(instance.context().initiator()).isEqualTo("1001");
        assertThat(instance.context().initiatorLong()).isEqualTo(1001L);
        assertThat(instance.context().initiator(Long.class)).isEqualTo(1001L);

        List<Task> tasks = taskService.findTasksByBusinessKey("purchase_order:" + orderId);
        assertThat(tasks).isNotEmpty();

        taskService.completeTask(tasks.get(0).id(), ApproveDecision.of(Actor.HumanActor.of("Manager_Bob"), "OK"));

        String status = jdbcTemplate.queryForObject("SELECT status FROM biz_purchase_order WHERE id = ?", String.class, String.valueOf(orderId));
        assertThat(status).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should throw clear exception when invalid entity type is passed to processEngine.start(Object)")
    void should_throw_when_invalid_entity_passed() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processEngine.start("some_raw_string"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a valid workflow entity");
    }
}
