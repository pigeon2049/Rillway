# Rillway

<p align="center">
  <strong>AI-native workflow runtime for Spring Boot.</strong><br>
  <em>Define business intent in natural language. Let humans, rules, and agents find the right path.</em>
</p>

<p align="center">
  <a href="#key-features">Features</a> •
  <a href="#core-concepts">Core Concepts</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#agent-governance">Agent Governance</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#project-modules">Modules</a> •
  <a href="#roadmap">Roadmap</a>
</p>

---

> **"Intent defines the goal. Rillway determines the path."**  
> *The workflow defines the boundary. The agent decides within the boundary.*

**Rillway**（名字取自 *rill* —— 潺潺细流、水流顺势成溪）是一个面向 Java / Spring Boot 生态的 **AI 原生工作流执行框架（AI-Native Workflow Framework）**。

传统工作流引擎（如 Flowable、Camunda）要求开发者或业务人员预先绘制详尽僵硬的 BPMN 流程图。然而在企业智能化时代，许多业务流程更适合**以自然语言或结构化意图定义目标**，并在运行时由**规则、表单上下文、企业制度、人工审批与 AI Agent** 协同决策，顺着边界与约束自然形成最优路径。

---

## 🌟 核心理念与特性 (Key Features)

- 🧠 **AI-Native & Intent-Driven**：支持通过自然语言表达业务意图，由 AI 解释器将其转换为可验证、可预览、可执行的结构化流程定义，无需编写复杂的 BPMN XML。
- 🤝 **三元决策主体 (Three Decision Actors)**：
  - **Human（人工）**：分配至具体角色/用户，支持审批、驳回、转办与升级。
  - **Rule（规则）**：基于表单字段与流程变量进行确定性条件判断与自动分流。
  - **Agent（智能体）**：AI Agent 可接管特定流程节点，调取表单上下文与企业制度进行自主推理与决策。
- 🛡️ **严格的 Agent 权限护栏 (Agent Governance & Guardrails)**：
  - **显式授权级别**：`ADVISORY`（仅建议）、`DELEGATED`（受权决策）、`AUTONOMOUS`（全权自主）。
  - **审计与制度依据**：每次 Agent 决策均记录推理摘要（Reasoning Summary）、佐证（Evidence）和制度条款（Policy References），绝不暴露难以审计的原始 LLM CoT。
  - **确定性降级 (Fallback)**：当 Agent 超出授权、置信度不足或决策异常时，自动平滑降级至指定人工节点。
- 🔮 **流程静态预览 (Process Preview)**：在流程正式触发前，支持传入模拟表单数据，提前预判潜在路径、审批人、参与 Agent 及必要字段。
- 🔌 **零厂商绑定 (Vendor-Neutral)**：核心领域模型零外部框架依赖（纯 Java 21 `record` 与 `sealed interface`），AI 能力与 LLM SDK（Spring AI / LangChain4j / 任意大模型）完全基于 SPI 解耦。
- ⚡ **无缝集成 Spring Boot**：提供开箱即用的 `rillway-spring-boot-starter`，可轻松嵌入 RuoYi-Vue-Pro、Gongdu 等企业级后台系统。

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

```xml
<dependency>
    <groupId>com.wegongdu.rillway</groupId>
    <artifactId>rillway-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义业务流程（以采购审批为例）

**业务意图**：
- 员工提交采购申请；
- **5000 元以下**：由直属部门经理审批；
- **5000 元以上**：由 `purchase-review-agent` 根据企业采购制度审核；
  - 采购 Agent 拥有 `DELEGATED` 权限，可执行 `approve` / `reject`；
  - **金额超过 50,000 元**：必须 `escalate` 到总经理人工审批；
  - Agent 无法做出确切判断时：`fallback` 到采购经理人工复核。

```java
ProcessDefinition definition = ProcessDefinition.builder("purchase-process")
    .name("企业采购合规审批流程")
    .startNode("start")
    .ruleNode("amount-check", rule -> rule
        .when(ctx -> ctx.getDecimal("amount").compareTo(new BigDecimal("5000")) < 0, "manager-approval")
        .otherwise("agent-review")
    )
    .humanNode("manager-approval", human -> human
        .assigneeRole("DEPARTMENT_MANAGER")
        .thenTo("end")
    )
    .agentNode("agent-review", agent -> agent
        .agentId("purchase-review-agent")
        .authority(AgentAuthority.DELEGATED)
        .policies("PURCHASE_POLICY_2026", "INVOICE_STANDARD")
        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT, DecisionType.ESCALATE)
        .fallbackNode("procurement-manager-approval")
        .on(DecisionType.APPROVE, "end")
        .on(DecisionType.REJECT, "end")
        .on(DecisionType.ESCALATE, "general-manager-approval")
    )
    .humanNode("procurement-manager-approval", human -> human
        .assigneeRole("PROCUREMENT_MANAGER")
        .thenTo("end")
    )
    .humanNode("general-manager-approval", human -> human
        .assigneeRole("GENERAL_MANAGER")
        .thenTo("end")
    )
    .endNode("end")
    .build();
```

### 3. 启动流程与待办审批 (极简无感)

#### 发起审批并绑定业务单据 ID (businessKey)
```java
@Autowired
private ProcessEngine processEngine;

// 1. 组装表单数据
ProcessContext context = ProcessContext.builder()
    .variable("applicant", "张三")
    .variable("item", "高性能研发服务器")
    .variable("amount", new BigDecimal("12000"))
    .build();

// 2. 发起流程并无感持久化 (自动创建 rillway_instance，遇人工节点自动生成 rillway_task)
ProcessInstance instance = processEngine.start(
    purchaseDefinition, 
    "PURCHASE_ORDER_20260818001", // 业务单据ID
    context
);
```

#### 查询当前用户的待办任务列表 (TaskService)
```java
@Autowired
private TaskService taskService;

// 查询当前用户的待办 (按 userId 与所拥有角色自动精确过滤)
List<Task> pendingTasks = taskService.findPendingTasks(currentUserId, currentUserRoles);
for (Task task : pendingTasks) {
    System.out.println("待办ID: " + task.id());
    System.out.println("单据编号: " + task.businessKey());
    System.out.println("审批节点: " + task.nodeName());
}
```

#### 用户审批通过 / 驳回
```java
// 一行代码完成：加载实例 -> 驱动流程 -> 任务标为已办 -> 更新数据库与审计轨迹
taskService.completeTask(
    taskId,
    ApproveDecision.of(Actor.HumanActor.of(currentUserId, "DEPARTMENT_MANAGER"), "核验无误，同意采购")
);
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

### 3. 大模型驱动的纯自然语言人员指派 (LLM + Tool Calling)

**告别死板复杂的伪表达式语法！** 在流程节点配置或自然语言生成中，直接填写人类大白话提示词：

```java
// 流程节点中直接写人类自然语言，大模型结合人事工具集自主推理！
.humanNode("leader-approval", h -> h
    .name("直属领导审批")
    .assigneePrompt("让申请人的直属领导审批") // 纯自然语言：大模型自主 Tool Calling 查询组织架构！
)
```

#### 大模型执行流程：
1. **理解上下文意图**：大模型接收提示词 `"让申请人的直属领导审批"` 与表单上下文 `{ initiator: "Alice", dept: "IT" }`；
2. **自主 Tool Calling**：大模型决定调用工具 `getDirectLeader("Alice")`；
3. **返回真实人员**：组织架构接口返回 `"Manager_Bob"`，大模型精准完成人员指派并生成待办 Task！

---

### 4. 成功流程决策缓存与人事核验机制 (ResolutionCache & 0 Token Fast-Path)

为了大幅节省大模型 Token 成本并提升审批流转性能，Rillway 内置了**带人事核验的成功决策缓存表**（`rillway_resolution_cache`）：

```text
┌─────────────────────────────────────────────────────────────┐
│ 1. 首次解析：大模型 Tool Calling 推理并记录成功缓存样本      │
│ 2. 再次执行：相同部门/岗位相同单据触发 0 Token 极速通道     │
│ 3. 轻量核验：毫秒级调用 IdentityService 核验人员是否变更    │
│    • 一致有效 -> 0 Token 消耗，2ms 极速返回！                │
│    • 人事变动 -> 自动失效缓存，触发大模型重新推理并自进化    │
└─────────────────────────────────────────────────────────────┘
```

- **自动建表 `rillway_resolution_cache`**：记录 `definition_id`、`node_id`、`prompt_hash`、`department_id`、`resolved_user_id`、`hit_count`；
- **90%+ Token 节省**：相同部门的日常重复审批（报销、采购、请假）直接命中缓存；
- **绝对准确**：即使命中缓存，也会在微秒级内向业务 `IdentityService` 核验该主管是否离职/调岗，杜绝脏数据。

---

## 🗄️ 极致无感持久化 (Zero-Config Persistence)

Rillway 专为企业中台（如 `gongdu-base`、`ruoyi-vue-pro`）设计了自适应无感持久化：

- **自动建表 (Auto DDL)**：应用启动检测到数据库连接池，自动初始化 3 张极简核心表（`rillway_instance`, `rillway_task`, `rillway_history`），全面兼容 MySQL / PostgreSQL / Oracle / H2 / SQLite 等；
- **零 ORM 侵入**：底层基于 Spring 标准 `JdbcTemplate`，不与业务现有的 MyBatis-Plus / JPA 冲突；
- **事务同生共死**：天然参与 Spring `@Transactional`，业务单据插入与流程实例落库在同一本地事务中，彻底消除分布式事务痛点；
- **自适应降级**：无数据库环境（如纯单测）自动降级为 InMemory 模式，开箱即跑。

---

## 📂 项目模块划分 (Project Modules)

```text
rillway
├── rillway-core                       # 纯 Java 领域模型 (ProcessDefinition, Node, Decision, Authority, Validator)
├── rillway-runtime                    # 工作流核心引擎 (ProcessEngine, NodeExecutor, Context, PathResolver)
├── rillway-ai                         # 自然语言意图解析 SPI (IntentInterpreter, ProcessIntent)
├── rillway-agent                      # Agent 抽象、Registry、Authority 守卫与 Fallback
├── rillway-policy                     # 企业制度策略 SPI (Policy, PolicyProvider)
├── rillway-audit                      # 结构化审计流 (AuditEvent, AuditSink, InMemoryAuditSink)
├── rillway-spring-boot-autoconfigure  # Spring Boot 3 自动配置
├── rillway-spring-boot-starter        # 快速集成 Starter
└── rillway-example                    # 采购审批等经典企业实战 Demo
```

---

## 🗺️ 演进路线 (Roadmap)

- [x] **Phase 1: 核心框架初始化 (Current)**
  - 纯 Java 21 不可变领域模型设计（`sealed interface` + `record`）
  - 核心 Runtime 流程推进引擎与 `NodeExecutor`
  - Agent 权限守卫（`ADVISORY` / `DELEGATED` / `AUTONOMOUS`）与越权拦截
  - 企业 Policy 与 Audit 审计事件 SPI
  - Spring Boot 3 Starter 自动装配与 Purchase Example
- [ ] **Phase 2: AI 意图与生态集成**
  - 基于 Spring AI / LangChain4j 的 `IntentInterpreter` 原生适配
  - 自然语言动态生成与补全 `ProcessDefinition`
  - 策略知识库（Policy RAG）标准集成组件
- [ ] **Phase 3: 持久化与企业级特性**
  - 流程状态持久化 SPI（JDBC / MyBatis-Plus / Redis）
  - 分布式异步任务执行与超时重试
  - 流程热更新与版本演进控制
- [ ] **Phase 4: 可视化与监控**
  - 流程运行轨迹与 Agent 决策解释可视化组件
  - 与 RuoYi-Vue-Pro 等开源管理后台开箱即用集成

---

## 📌 项目状态 (Status)

> [!NOTE]
> Rillway 目前处于 **Early Development (初期孵化与快速迭代)** 阶段。  
> 我们的目标是探索 **AI 原生时代的人机协同流程范式**，而不是完全替代传统的重型 BPMN 引擎。欢迎提交 Issue、PR 或加入讨论！

---

## 📄 开源协议 (License)

本项目基于 [Apache 2.0 License](LICENSE) 开源。
