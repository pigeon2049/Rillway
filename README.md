# Rillway

<p align="center">
  <strong>AI-native workflow runtime for Spring Boot.</strong><br>
  <em>Define business intent in natural language. Let humans, rules, and agents find the right path.</em>
</p>

<p align="center">
  <a href="#key-features">Features</a> •
  <a href="#core-concepts">Core Concepts</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#zero-code-binding">Zero-Code Binding</a> •
  <a href="#identity-and-ai">Identity & AI Dispatching</a> •
  <a href="#resolution-cache">Resolution Cache</a> •
  <a href="#agent-governance">Agent Governance</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#roadmap">Roadmap</a>
</p>

---

> **"Intent defines the goal. Rillway determines the path."**  
> *The workflow defines the boundary. The agent decides within the boundary.*

**Rillway**（名字取自 *rill* —— 潺潺细流、水流顺势成溪）是一个面向 Java / Spring Boot 生态的 **AI 原生工作流执行框架（AI-Native Workflow Framework）**。

传统工作流引擎（如 Flowable、Camunda）要求开发者或业务人员预先绘制详尽僵硬的 BPMN 流程图。然而在企业智能化时代，许多业务流程更适合**以自然语言或结构化意图定义目标**，并在运行时由**规则、表单上下文、企业制度、人工审批与 AI Agent** 协同决策，顺着边界与约束自然形成最优路径。

---

## 🌟 核心理念与特性 (Key Features)

- 🧠 **AI-Native & Intent-Driven**：支持通过人类大白话自然语言表达业务意图与审批人指派，由大模型（LLM）+ 组织架构 Tool Calling 自主推理完成指派，彻底告别死板复杂的伪表达式语法。
- 🏢 **零代码单据接入与状态自动回写 (Zero-Code Binding)**：配置表驱动，流程审批通过/驳回时引擎自动回写业务物理表状态字段，业务开发者无需编写任何 Listener 或 Mapper 代码。
- 📢 **流程事件监听与多渠道通知联动 (Process Event & Notification)**：原生桥接 Spring 事件总线，业务开发通过标准 `@EventListener` 即可拦截流程发起、节点流转、审批通过/驳回，轻松对接飞书、企微、钉钉、邮件及下游 MQ / ERP。
- 👥 **全维组织架构身份画像 (Multi-Dimensional UserProfile SPI)**：提供标准人事 SPI，让流程和大模型无缝感知发起人的部门、岗位、职务、职级与直属领导，支持跨部门差异化智能审批流转。
- ⚡ **成功决策缓存与条件分支隔离 (ResolutionCache & 0 Token Fast-Path)**：
  - **组织架构快照核验**：记录双方人员部门/岗位指纹与 7 天有效期，未变动时触发 0 Token 毫秒级极速复用；
  - **条件分支指纹隔离**：针对“请假大于3天总经理审批，否则部门主管审批”等多分支场景，自动根据表单变量计算分支指纹，彻底杜绝不同额度/天数单据缓存串用。
- 🤝 **三元决策主体 (Three Decision Actors)**：
  - **Human（人工）**：分配至具体角色/用户，支持审批、驳回、转办与升级。
  - **Rule（规则）**：基于表单字段与流程变量进行确定性条件判断与自动分流。
  - **Agent（智能体）**：AI Agent 可接管特定流程节点，调取表单上下文与企业制度进行自主推理与决策。
- 🛡️ **严格的 Agent 权限护栏 (Agent Governance & Guardrails)**：
  - **显式授权级别**：`ADVISORY`（仅建议）、`DELEGATED`（受权决策）、`AUTONOMOUS`（全权自主）。
  - **审计与制度依据**：每次 Agent 决策均记录推理摘要（Reasoning Summary）、佐证（Evidence）和制度条款（Policy References），绝不暴露难以审计的原始 LLM CoT。
  - **确定性降级 (Fallback)**：当 Agent 超出授权、置信度不足或决策异常时，自动平滑降级至指定人工节点。
- 🗄️ **极致无感持久化 (Zero-Config Persistence)**：应用启动自动初始化 5 张核心表，支持与业务同一本地事务（`@Transactional`）同生共死。
- 🔌 **零厂商绑定 (Vendor-Neutral)**：核心领域模型零外部框架依赖（纯 Java 21 `record` 与 `sealed interface`），AI 能力与 LLM SDK（Spring AI / LangChain4j / 任意大模型）完全基于 SPI 解耦。

---

## 🏛️ 三大决策主体 (Three Decision Actors)

```text
                     ┌───────────────┐
                     │ Process Start │
                     └───────┬───────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
     ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
     │  👨 Human   │  │   📏 Rule   │  │  🤖 Agent   │
     │  Approval   │  │ Logic/Route │  │ Takeover    │
     └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
            │                │                │
            │ (Reason)       │ (Condition)    │ (Policy + Guardrail)
            └────────────────┼────────────────┘
                             │
                             ▼
                     ┌───────────────┐
                     │ Process Audit │
                     └───────────────┘
```

| 决策主体 | 适用场景 | 输出特性 | 失败处理 |
| :--- | :--- | :--- | :--- |
| **Human** | 核心财务、重要合同、最终责任确认 | 人工审批意见、签字理由 | 超时提醒、转办催办 |
| **Rule** | 金额分流、状态校验、确定性业务准入 | 布尔条件匹配、目标分支路由 | 命中默认兜底分支 |
| **Agent** | 制度合规审查、发票核验、辅助资质初审 | 决策结果 + 推理摘要 + 制度依据 + 置信度 | 自动 Fallback 到人工复核 |

---

## 🔐 Agent 权限与治理体系 (Agent Governance)

在 Rillway 中，Agent 的决策权属于**领域层硬性约束（Domain Constraint）**，而非 Prompt 中的一段提示词。Runtime 引擎会在运行时对 Agent 决策进行强校验：

```java
public enum AgentAuthority {
    /** 建议型：Agent 仅输出分析建议，必须由人工做最终确认 */
    ADVISORY,

    /** 受权决策型：Agent 仅在指定允许的决策范围（如 approve/reject）内直接决定 */
    DELEGATED,

    /** 全权自主型：Agent 可在授权范围内自主决定当前结果与后续流转分支 */
    AUTONOMOUS
}
```

```text
[Agent Node Triggered]
        │
        ▼
   [Query Policies & Form Context]
        │
        ▼
   [Agent Inference & Decision]
        │
        ├─► Is Decision in Allowed Decisions? ── No ──► [Fallback to Human]
        ├─► Is Decision within Authority?    ── No ──► [Fallback to Human]
        ├─► Is Confidence Sufficient?        ── No ──► [Fallback to Human]
        │
       Yes
        │
        ▼
   [Record Audit with Reasoning Summary & Policy References]
        │
        ▼
   [Advance Workflow]
```

---

## 🚀 快速上手 (Quick Start)

### 1. 引入 Maven Starter

<dependency>
    <groupId>com.wegongdu.rillway</groupId>
    <artifactId>rillway-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

### 2. 配置大模型（原生 HTTP 零依赖 / 配置文件 & 数据库双轨）

Rillway 内置了轻量级原生 HTTP 客户端，**零第三方 AI 框架重型依赖**（底层纯 Java 原生 `HttpClient`），天然兼容所有标准 OpenAI 协议大模型（**DeepSeek、通义千问 Qwen、Kimi、GLM、Ollama、OpenAI** 等）：

#### 方式 A：在 `application.yml` 中配置
```yaml
rillway:
  enabled: true
  ai:
    openai:
      enabled: true
      base-url: https://api.deepseek.com/v1     # 支持 DeepSeek / DashScope / 本地 Ollama 等
      api-key: ${DEEPSEEK_API_KEY:sk-xxxxxxxx}  # 模型 API Key
      model: deepseek-v4-flash                     # 模型名称
      temperature: 0.1
      timeout-seconds: 30
```

#### 方式 B：在数据库配置表 (`rillway_ai_config`) 中配置（支持后台动态热切）
应用启动时自动初始化 `rillway_ai_config` 表，支持在管理后台或 SQL 中维护模型连接，**无需重启应用**：
```sql
INSERT INTO rillway_ai_config (
    id, provider_name, base_url, api_key, model_name, temperature, timeout_seconds, is_default, enabled, updated_at
) VALUES (
    'cfg_deepseek_01',
    'deepseek',
    'https://api.deepseek.com/v1',
    'sk-xxxxxxxx',
    'deepseek-v4-flash',
    0.1,
    30,
    true,
    true,
    CURRENT_TIMESTAMP
);
```

---

### 3. 方式一（首选推荐）：零代码配置表 + 自然语言意图驱动

> 💡 **核心优势**：流程变动（如修改审批阈值、调整审核角色）**无需修改任何 Java 代码，无需发版重启**，仅需在数据库或管理后台更新一段 Prompt！

#### Step 1: 在配置表 (`rillway_binding_config`) 中填入大白话意图

```sql
INSERT INTO rillway_binding_config (
    id, 
    business_type, 
    process_prompt, 
    table_name, 
    status_column, 
    approved_value, 
    rejected_value, 
    enabled
) VALUES (
    'cfg_purchase_01',
    'purchase_order',
    '员工提交采购申请：
     1. 金额小于 5000 元时，由申请人直属部门经理审批；
     2. 金额在 5000 到 50,000 元且有发票时，由 AI 采购合规 Agent 依据企业采购制度直接自动审批；
     3. 金额大于 50,000 元时，必须升级至总经理人工审批；
     4. 任何无法确定或缺少发票情况，由采购经理人工复核。',
    'biz_purchase_order',
    'status',
    'APPROVED',
    'REJECTED',
    true
);
```

#### Step 2: 业务系统直接传 Bean 发起审批（1 行代码！）

业务代码**无需声明任何 ProcessDefinition，无需手动拆解字段**，直接将单据实体（MyBatis-Plus Entity / JPA 实体 / 普通 POJO / Record）丢给引擎，底层全自动匹配配置表、解析单据 ID、发起人与表单变量：

```java
// 1. 业务单据实体（支持 MyBatis-Plus @TableName / JPA @Table / @RillwayEntity）
@TableName("biz_purchase_order")                  // 物理表名：匹配 rillway_binding_config.table_name
@RillwayEntity(businessType = "purchase_order")   // 单据类型：匹配 rillway_binding_config.business_type
public class PurchaseOrder {

    @TableId
    @EntityId
    private Long id;              // 🌟 单据主键 ID (如 18247291823791283L)

    @ProcessInitiator
    private Long creatorUserId;   // 🌟 发起人用户 ID (如 1001L，彻底防重名)

    private BigDecimal amount;    // 12000
    private String item;          // "高性能研发服务器"
    private Boolean hasInvoice;   // true

    @ProcessIgnore
    private String password;      // 🛡️ 敏感字段：自动跳过，绝不进入流程上下文与大模型
}

// 🚀 2. 终极极简：直接扔 Bean！一行代码全自动发起审批流
ProcessInstance instance = processEngine.start(order);
```

#### Step 3: 人工待办审批与全自动状态回写

```java
@Autowired
private TaskService taskService;

// 1. 查询当前经理待办（传入当前用户 ID / 角色）
List<Task> pendingTasks = taskService.findPendingTasks(currentUserId, currentUserRoles);

// 2. 用户审批通过
taskService.completeTask(
    taskId,
    ApproveDecision.of(Actor.HumanActor.of(currentUserId, "DEPARTMENT_MANAGER"), "核验无误，同意采购")
);

// 🎉 3. 审批完成后，Rillway 自动将物理表 biz_purchase_order 对应行 status 回写为 'APPROVED'！
```

---

### 4. 方式二（进阶极客）：Java Fluent DSL 编程式定义

适合用于确定性本地单元测试，或对微秒级静态拓扑有强约束的场景：

```java
ProcessDefinition definition = ProcessDefinition.builder("purchase-approval-workflow")
    .name("企业采购合规审批流程")
    .startNode("start", "提交申请")
    .ruleNode("amount-check", rule -> rule
        .name("金额阈值初筛")
        .when("低于5000由经理审批", ctx -> {
            BigDecimal amt = ctx.getDecimal("amount");
            return amt != null && amt.compareTo(new BigDecimal("5000")) < 0;
        }, "manager-approval")
        .otherwise("agent-review")
    )
    .humanNode("manager-approval", human -> human
        .name("直属经理审批")
        .assigneeRole("DEPARTMENT_MANAGER")
    )
    .agentNode("agent-review", agent -> agent
        .name("采购智能审核")
        .agentId("purchase-review-agent")
        .authority(AgentAuthority.DELEGATED)
        .policies("PURCHASE_POLICY_2026", "INVOICE_STANDARD")
        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT, DecisionType.ESCALATE)
        .fallbackNodeId("procurement-manager-approval")
        .on(DecisionType.APPROVE, "end")
        .on(DecisionType.REJECT, "end")
        .on(DecisionType.ESCALATE, "general-manager-approval")
    )
    .humanNode("procurement-manager-approval", human -> human
        .name("采购经理人工复核 (Fallback)")
        .assigneeRole("PROCUREMENT_MANAGER")
    )
    .humanNode("general-manager-approval", human -> human
        .name("总经理审批 (Escalated)")
        .assigneeRole("GENERAL_MANAGER")
    )
    .endNode("end", "审批结束")
    .edge("start", "amount-check")
    .edge("manager-approval", "end")
    .edge("procurement-manager-approval", "end")
    .edge("general-manager-approval", "end")
    .build();

// 启动编程式流程
processEngine.start(definition, "purchase_order:PO_20260818001", context);
```

---

## 🏢 零代码单据接入与状态自动回写 (Zero-Code Binding)

在传统 BPMN 引擎中，每个单据接入审批都要写 Listener 或 Service 手动更新单据状态。Rillway 提供**配置表驱动的零代码自动回写**：

### 1. 预置单据配置表 (`rillway_binding_config`)
应用启动时自动建表，支持在数据库或管理后台配置业务单据与审批状态的映射：

| 字段 | 含义 | 示例值 | 说明 |
| :--- | :--- | :--- | :--- |
| `business_type` | 业务单据类型 | `purchase_order` | 单据唯一标识 |
| `process_definition_id` | 绑定的流程ID | `purchase-approval-workflow` | 绑定的流程定义 |
| `table_name` | 业务物理表名 | `biz_purchase_order` | 业务单据主表 |
| `primary_key_column` | 主键列名 | `id` | 默认 `id` |
| `status_column` | 状态字段列名 | `status` | 待更新的状态字段 |
| `approved_value` | 审批通过回写值 | `APPROVED` 或 `2` | 流程通过时自动回写 |
| `rejected_value` | 审批驳回回写值 | `REJECTED` 或 `3` | 流程驳回时自动回写 |

### 2. 零代码自动回写效果
业务提交单据并发起流程：
```java
// 绑定单据格式：business_type:entity_id
processEngine.start(definition, "purchase_order:PO_20260818001", context);
```
当流程节点审批通过（或被驳回）时，Rillway 自动执行：
```sql
UPDATE biz_purchase_order 
SET status = 'APPROVED' 
WHERE id = 'PO_20260818001';
```
**业务开发者无需编写任何监听器或 Mapper 更新代码，单据状态完全自动化流转！**

---

## 📢 流程事件监听与通知联动 (Event Listener)

对于状态回写之外的**横切关注点**（如：给飞书/企微/钉钉发待办卡片、流程结束时调用外部 ERP 或发送 MQ 领域事件），Rillway 原生集成了 Spring 事件机制。业务开发人员只需使用标准 `@EventListener` 即可轻松监听全生命周期事件：

```java
@Component
public class PurchaseWorkflowNotificationListener {

    @Autowired
    private FeishuNotificationService feishuService;
    @Autowired
    private ErpIntegrationService erpService;

    /**
     * 1. 流程发起时：通知发起人已进入审批流程
     */
    @EventListener
    public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        feishuService.sendText(event.initiator(), 
            "您的单据 [" + event.businessKey() + "] 已成功提交审批。");
    }

    /**
     * 2. 到达某节点时：给对应审批人/角色发送待办提醒卡片
     */
    @EventListener
    public void onNodeEntered(ProcessEvent.NodeEnteredEvent event) {
        if (event.assigneeRole() != null) {
            feishuService.sendApprovalCard(
                event.assigneeRole(), 
                "【待办提醒】单据 [" + event.businessKey() + "] 等待您审批", 
                event.processInstanceId()
            );
        }
    }

    /**
     * 3. 流程结束时（审批通过 / 驳回）：发送结果通知并触发下游系统联动
     */
    @EventListener
    public void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
        if (event.isSuccess()) {
            feishuService.sendSuccessNotice(event.businessKey(), "审批已全部通过！");
            // 触发下游微服务或 MQ 领域事件
            erpService.createPurchaseOrder(event.businessKey());
        } else {
            feishuService.sendRejectNotice(event.businessKey(), "单据审批已被驳回。");
        }
    }
}
```

> 💡 **提示**：除了 Spring `@EventListener` 注解外，非 Spring 环境或编程式开发者也可直接实现 `ProcessEventListener` 接口进行事件订阅。

---

## 👥 极简组织架构与人员画像 SPI (`IdentityService`)

为了让流程和大模型无缝感知**“发起人是谁、在哪个部门、什么岗位”**，并支持针对不同部门的差异化审批，Rillway 提供全维组织画像 SPI：

### 1. 发起人多维组织画像 (`UserProfile`)
```java
package com.wegongdu.rillway.core.identity;

public record UserProfile(
    String userId,           // 用户ID (如 "Alice")
    String username,         // 用户姓名 (如 "爱丽丝")
    String departmentId,     // 所属部门编码 (如 "DEPT_RD")
    String departmentName,   // 部门名称 (如 "核心研发部")
    String postCode,         // 岗位编码 (如 "DEV_LEAD")
    List<String> roles,      // 角色列表
    String directLeaderId,   // 直属领导ID
    Map<String, Object> extraAttributes // 扩展属性(职级等)
) implements Serializable {}
```

### 2. SPI 接口定义
```java
package com.wegongdu.rillway.core.identity;

public interface IdentityService {
    /** 核心方法：获取用户完整组织身份画像 */
    Optional<UserProfile> getUserProfile(String userId);

    /** 查询用户的直属上级 */
    Optional<String> getDirectLeader(String userId);

    /** 查询部门负责人/经理 */
    Optional<String> getDepartmentManager(String departmentId);

    /** 查询指定岗位下的所有人员 */
    List<String> getUsersByPost(String postCode);

    /** 查询指定角色下的所有人员 */
    List<String> getUsersByRole(String roleCode);

    /** 查询指定部门下的所有人员 */
    List<String> getUsersByDepartment(String departmentId);
}
```

### 3. 在业务项目 (`gongdu-base` / `ruoyi-vue-pro`) 中极简适配
只需写一个适配类对接业务现有的 `sys_user` / `sys_dept` 表：
```java
@Component
public class SystemIdentityAdapter implements IdentityService {
    @Autowired
    private SysUserService userService;
    @Autowired
    private SysDeptService deptService;

    @Override
    public Optional<UserProfile> getUserProfile(String userId) {
        SysUser user = userService.getById(userId);
        if (user == null) return Optional.empty();
        return Optional.of(UserProfile.builder(userId)
            .username(user.getNickname())
            .departmentId(user.getDeptId())
            .postCode(user.getPostCode())
            .directLeaderId(user.getLeaderUserId())
            .build());
    }

    @Override
    public Optional<String> getDepartmentManager(String departmentId) {
        return Optional.ofNullable(deptService.getLeaderUserId(departmentId));
    }
    // ...
}
```

---

## 🤖 大模型驱动的纯自然语言人员指派 (LLM + Tool Calling)

**彻底告别死板复杂的伪表达式语法！** 在流程节点配置或自然语言生成中，直接填写人类大白话提示词：

```java
// 流程节点中直接写人类自然语言，大模型结合人事工具集自主推理！
.humanNode("leader-approval", h -> h
    .name("直属领导审批")
    .assigneePrompt("让申请人的直属领导审批") // 纯自然语言：大模型自主 Tool Calling 查询组织架构！
)
```

#### 大模型执行流程：
1. **理解上下文意图**：大模型接收提示词 `"让申请人的直属领导审批"` 与表单上下文 `{ initiator: "Alice", dept: "DEPT_RD" }`；
2. **自主 Tool Calling**：大模型决定调用工具 `getDirectLeader("Alice")` 或 `getUserProfile("Alice")`；
3. **返回真实人员**：组织架构接口返回 `"Manager_Bob"`，大模型精准完成人员指派并生成待办 Task！

---

## ⚡ 成功流程决策缓存与条件分支隔离 (ResolutionCache)

为了大幅节省大模型 Token 成本并提升审批流转性能，Rillway 实现了**基于组织架构快照与条件分支指纹隔离的决策缓存机制**（`rillway_resolution_cache`）：

```text
┌────────────────────────────────────────────────────────────────────────┐
│  提示词: "如果请假天数大于3天由总经理审批，否则由部门负责人审批"        │
│                                                                        │
│  • Alice 请假 1 天 (leaveDays=1)                                       │
│    -> 提取分支指纹: leaveDays<=3，指派部门主管，写入槽位 [leaveDays<=3] │
│  • Bob 请假 5 天 (leaveDays=5)                                         │
│    -> 提取分支指纹: leaveDays>3，绝不误用 1 天的缓存！指派总经理并独立缓存 │
│  • Alice 再次请假 2 天 -> 0 Token 毫秒级命中槽位 [leaveDays<=3] (主管)  │
│  • Bob 再次请假 10 天  -> 0 Token 毫秒级命中槽位 [leaveDays>3] (总经理) │
└────────────────────────────────────────────────────────────────────────┘
```

- **组织架构快照核验 (Snapshot Verification)**：
  缓存记录发起人与审批人双方的部门/岗位指纹与 7 天有效期。流转时仅需客观比对双方人事状态是否发生变动，**不依赖任何脆弱的 Prompt 关键词硬编码**！
- **条件分支指纹隔离 (Branch Fingerprint Isolation)**：
  自动根据表单变量提取 `conditionBranchKey`（如 `leaveDays<=3` 与 `leaveDays>3`），不同条件分支落入独立缓存槽位，**彻底消除不同额度/天数单据串用缓存的重大风险**！
- **企业最佳实践推荐**：
  - **显式流程规则分流**：对于硬性制度红线（如报销额度、假期间隔），推荐直接在流程图中使用 `.edge("start", "gm-approval", ctx -> ctx.getInt("days") > 3)`，可视化度与可解释性最高；
  - **AI 动态分支隔离**：对于自然语言动态策略，Rillway 自动计算分支指纹安全隔离。

---

## 🗄️ 极致无感持久化 (Zero-Config Persistence)

Rillway 专为企业中台（如 `gongdu-base`、`ruoyi-vue-pro`）设计了自适应无感持久化：

- **自动建表 (Auto DDL)**：应用启动检测到数据库连接池，自动初始化 5 张极简核心表：
  1. `rillway_instance`：流程实例与上下文快照
  2. `rillway_task`：人工待办与候选人任务
  3. `rillway_history`：执行历史轨迹与审计事件
  4. `rillway_binding_config`：零代码业务单据绑定表
  5. `rillway_resolution_cache`：大模型决策快照与分支缓存表
- **零 ORM 侵入**：底层基于 Spring 标准 `JdbcTemplate`，不与业务现有的 MyBatis-Plus / JPA 冲突；
- **事务同生共死**：天然参与 Spring `@Transactional`，业务单据插入与流程实例落库在同一本地事务中，彻底消除分布式事务痛点；
- **自适应降级**：无数据库环境（如纯单测）自动降级为 InMemory 模式，开箱即跑。

---

## 📂 项目模块划分 (Project Modules)

```text
rillway
├── rillway-core                       # 纯 Java 领域模型 (ProcessDefinition, Node, Decision, Authority, UserProfile)
├── rillway-runtime                    # 工作流核心引擎 (ProcessEngine, NodeExecutor, Context, TaskService)
├── rillway-ai                         # 大模型驱动人员解析、ResolutionCache 缓存与 Tool Calling SPI
├── rillway-agent                      # Agent 抽象、Registry、Authority 守卫与 Fallback
├── rillway-policy                     # 企业制度策略 SPI (Policy, PolicyProvider)
├── rillway-audit                      # 结构化审计流 (AuditEvent, AuditSink, InMemoryAuditSink)
├── rillway-spring-boot-autoconfigure  # Spring Boot 3 自动配置 (自动建表、JDBC 仓储、状态回写)
├── rillway-spring-boot-starter        # 快速集成 Starter
└── rillway-example                    # 采购审批、零代码单据回写、大模型快照缓存等全套实战 Demo
```

---

## 🗺️ 演进路线 (Roadmap)

- [x] **Phase 1: 核心框架与治理体系**
  - 纯 Java 21 不可变领域模型设计（`sealed interface` + `record`）
  - 核心 Runtime 流程推进引擎与 `NodeExecutor`
  - Agent 权限守卫（`ADVISORY` / `DELEGATED` / `AUTONOMOUS`）与越权拦截
  - 企业 Policy 与 Audit 审计事件 SPI
- [x] **Phase 2: 企业中台无感集成与零代码绑定**
  - 自动建表与自适应 JDBC 事务同生共死持久化
  - `TaskService` 待办与已办任务中心
  - `rillway_binding_config` 零代码业务单据状态自动回写器
  - `UserProfile` 发起人全维组织架构画像 SPI
- [x] **Phase 3: AI-Native 智能调度与决策缓存**
  - 大模型驱动的纯自然语言人员指派（LLM + Tool Calling）
  - 组织架构快照客观核验（Snapshot-based Verification & 7天 TTL）
  - 自然语言条件分支指纹隔离机制（`condition_branch_key`）
  - 0 Token 毫秒级 Fast-Path 极速通道与自愈能力
- [ ] **Phase 4: 可视化与监控**
  - 流程运行轨迹与 Agent 决策解释可视化组件
  - 与 RuoYi-Vue-Pro / Gongdu 等开源管理后台前端一键集成

---

## 📌 项目状态 (Status)

> [!NOTE]
> Rillway 目前处于 **Active Fast Evolution (活跃演进)** 阶段。  
> 我们的目标是探索 **AI 原生时代的人机协同流程范式**，打造极简、高效、与 Spring Boot 中台零摩擦融合的工作流引擎。欢迎提交 Issue、PR 或加入讨论！

---

## 📄 开源协议 (License)

本项目基于 [Apache 2.0 License](LICENSE) 开源。
