<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3+-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot 3" />
  <a href="https://jitpack.io/#pigeon2049/Rillway"><img src="https://img.shields.io/badge/JitPack-v0.1.0-brightgreen?style=for-the-badge&logo=apachemaven" alt="JitPack" /></a>
  <img src="https://img.shields.io/badge/Architecture-AI--Native-6366F1?style=for-the-badge" alt="AI Native" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge" alt="License" />
</p>

<h1 align="center">⚡ Rillway: 面向中台的 AI 原生工作流引擎</h1>

<p align="center">
  <b>抛弃繁琐 XML 与笨重 BPMN。直接用自然语言描述制度，直接扔实体 Bean 发起，零代码单据状态回写，0 Token 决策缓存与金融级断网自愈。</b>
</p>

<p align="center">
  <a href="README.md"><b>简体中文</b></a> | <a href="README_EN.md"><b>English</b></a>
</p>

---

## 💡 为什么选择 Rillway？

传统工作流引擎（Activiti, Flowable, Camunda）在现代 Spring Boot 中台与大模型落地中存在明显痛点：
- **接入成本高**：需要画庞大的 BPMN XML，流程变量必须手动 `map.put()` 逐个组装；
- **业务耦合重**：状态回写需要写各种 Listener、Delegate，无法天然融入本地业务事务；
- **AI 落地难**：大模型调用容易超时/宕机、Token 昂贵，缺乏组织架构快照核验与决策缓存机制。

**Rillway 专为解决上述痛点而生：**
1. **自然语言定义制度**：用自然语言直接描述企业制度，无需绘制任何 BPMN 流程图；
2. **直接扔实体 Bean 发起**：一行代码传入单据实体，自动提取表名、雪花主键、发起人，自动脱敏；
3. **零代码单据状态回写**：配置驱动，审批通过/驳回自动更新业务单据状态，本地事务同生共死；
4. **0 Token 决策快照缓存**：自动核验组织架构快照与条件分支隔离，毫秒级极速命中，不产生 Token 费用；
5. **大模型断网自愈**：LLM 供应商 502/超时时，自动降级为语义顺延与人工安全复核，业务永不卡死；
6. **全链路追溯审计**：自动建表 `rillway_ai_trace`，记录每次 LLM 报文、耗时、Token 与 Java 方法入参。

---

## 🚀 正常接入步骤 (5步极速上手)

遵循标准工程实践，只需 **5 步** 即可在你的业务工程中跑通完整的 AI 审批流：

### 第 1 步：引入 Maven 依赖 (JitPack)

在业务工程的 `pom.xml` 中添加 JitPack 仓库与 Starter 依赖：

```xml
<!-- 1. 引入 JitPack 仓库 -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- 2. 引入 Rillway Spring Boot Starter -->
<dependencies>
    <dependency>
        <groupId>com.github.pigeon2049.Rillway</groupId>
        <artifactId>rillway-spring-boot-starter</artifactId>
        <version>v0.1.1</version>
    </dependency>
</dependencies>
```

---

### 第 2 步：配置大模型与追踪参数 (`application.yml`)

在配置文件中填入大模型连接信息（天然兼容 DeepSeek、通义千问、OpenAI、Ollama 等任何 OpenAI 协议接口）：

```yaml
rillway:
  enabled: true
  ai:
    openai:
      enabled: true
      base-url: https://api.deepseek.com/v1   # 或阿里云 DashScope / 微软 Azure 接口
      api-key: sk-your-api-key-here           # 你的 API Key
      model: deepseek-chat                    # 调用的模型名称 (如 deepseek-chat, qwen-plus)
      temperature: 0.1
      timeout-seconds: 30
    trace:
      enabled: true                           # 🌟 开启大模型与方法调用全链路追溯审计
      log-payload: true                       # 记录完整 Prompt 与 Response 报文
      record-tool-calls: true                 # 记录触发的 Java 方法名、入参 JSON 与返回值
```
> 💡 **提示**：也可以不写在 yml 里，直接向数据库表 `rillway_ai_config` 插入记录实现热插拔免重启。

---

### 第 3 步：对接企业组织架构与人员（两种方式任选其一）

#### 🌟 方式 A（推荐·零代码）：直接注册系统实体类的 Class (`OrgEntityRegistry`)

**完全无需手写任何 Mapper、SQL 或 Service 实现！** 开发者只需将项目中已有的用户、部门、角色或岗位实体 Class 注册为一个 Spring Bean，引擎通过**反射与 DDL 注解自省器（`EntityClassIntrospector`）**，全自动推导表名、主键、列映射及 Swagger `@Schema` 注释，并直接供大模型理解与调用：

```java
@Configuration
public class WorkflowOrgConfig {

    @Bean
    public OrgEntityRegistry orgEntityRegistry() {
        return OrgEntityRegistry.builder()
                .userEntity(SystemUserDO.class)      // 员工/用户实体
                .deptEntity(SystemDeptDO.class)      // 部门实体
                .roleEntity(SystemRoleDO.class)      // 角色实体
                .postEntity(SystemPostDO.class)      // 岗位实体（可选）
                .build();
    }
}
```

---

#### 方式 B：自定义实现 `IdentityService` SPI 接口（适合跨微服务 RPC）

如果系统需要通过 Feign / RPC 远程调用用户中台，可选择实现 `IdentityService` 接口：

```java
@Service
public class EnterpriseIdentityService implements IdentityService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptMapper deptMapper;

    // 1. 获取用户完整组织画像（部门ID、岗位、直属领导ID等）
    @Override
    public Optional<UserProfile> getUserProfile(String userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Optional.empty();

        return Optional.of(UserProfile.builder(userId)
                .username(user.getNickname())
                .departmentId(String.valueOf(user.getDeptId()))
                .departmentName(user.getDeptName())
                .directLeaderId(String.valueOf(user.getLeaderId()))
                .roles(user.getRoleKeys())
                .build());
    }

    // 2. 查询申请人的直属上级领导 ID
    @Override
    public Optional<String> getDirectLeader(String userId) {
        return Optional.ofNullable(userMapper.selectLeaderIdByUserId(userId));
    }

    // 3. 查询指定部门的主管 / 部门经理 ID
    @Override
    public Optional<String> getDepartmentManager(String departmentId) {
        return Optional.ofNullable(deptMapper.selectLeaderIdByDeptId(departmentId));
    }

    // 4. 根据角色或岗位查询人员列表（如 'ROLE_HRBP'）
    @Override
    public List<String> getUsersByRole(String roleCode) {
        return userMapper.selectUserIdsByRole(roleCode);
    }

    @Override
    public List<String> getUsersByDepartment(String departmentId) {
        return userMapper.selectUserIdsByDeptId(departmentId);
    }

    @Override
    public List<String> getUsersByPost(String postCode) {
        return userMapper.selectUserIdsByPostCode(postCode);
    }
}
```
> 💡 **开箱即用**：若业务工程未配置以上任何内容，Rillway 会自动启用内存版 `DefaultIdentityService`，单测与本地快速验证零门槛！

---

### 第 4 步：配置自然语言审批制度与状态回写 (`rillway_binding_config`)

应用启动时会自动检测并初始化该表。向表中配置你的**自然语言制度**与**绑定的单据表名**：

```sql
INSERT INTO rillway_binding_config (
    id, 
    business_type,      -- 绑定的业务类型或表名 (如 biz_purchase_order)
    process_prompt,     -- 🌟 自然语言制度：大模型自动理解并执行流转规则
    table_name,         -- 绑定的业务表名
    status_column,      -- 需要回写的状态字段
    approved_value,     -- 终审通过时的状态值 (如 APPROVED)
    rejected_value,     -- 审批驳回时的状态值 (如 REJECTED)
    enabled
) VALUES (
    'cfg_purchase_01',
    'biz_purchase_order',
    '员工提交采购申请：
     1. 金额小于 5,000 元时，由申请人直属部门主管审批；
     2. 金额在 5,000 到 50,000 元且有合规发票时，由 AI 采购合规 Agent 自动依据制度审批；
     3. 金额大于 50,000 元或缺少发票时，必须升级至部门总监与总经理审批。',
    'biz_purchase_order',
    'status',
    'APPROVED',
    'REJECTED',
    true
);
```

---

### 第 5 步：定义业务实体 Bean 并直接发起流程

#### ① 实体类定义（直接复用业务 Bean + 注解增强）
```java
@Data
@TableName("biz_purchase_order") // 🌟 兼容 MyBatis-Plus @TableName / JPA @Table，自动关联配置表
@RillwayEntity(businessType = "biz_purchase_order") // 可选：显式指定业务类型（默认使用表名）
public class PurchaseOrder {

    @TableId // 🌟 兼容 MyBatis-Plus @TableId、JPA @Id 或 Rillway @EntityId
    private Long id; // 支持 Long 雪花 ID 或 String 业务单号

    @ProcessInitiator // 🌟 声明流程发起人（支持 Long userId 或 String username）
    private String applicant;

    private BigDecimal amount;       // 自动转为流程变量 context.variable("amount", 12000)
    private Boolean hasInvoice;      // 自动转为流程变量 context.variable("hasInvoice", true)

    @ProcessVariable("dept_code")    // 可选：自定义流程变量 Key
    private String departmentCode;

    @ProcessIgnore                   // 🌟 忽略敏感字段（密码、密钥、大对象自动脱敏）
    private String internalRemark;
}
```

#### ② 一行代码发起审批（直接传 Bean）
```java
@Autowired
private ProcessEngine processEngine;

// 🚀 直接传 Bean！
// 引擎自动根据表名匹配制度、提取主键与变量，启动流程并在流转结束时自动将 status 回写为 APPROVED！
ProcessInstance instance = processEngine.start(purchaseOrder);
```

---

## 🎯 核心技术机制深度解析

### 1. 0 Token 决策快照缓存机制 (ResolutionCache)

为了解决大模型调用**慢（秒级）、贵（Token消耗）、不稳定（断网）**的问题，Rillway 设计了**决策快照缓存体系**：

```text
           [提交业务单据]
                 │
      [自动识别条件分支指纹] ── (如: amount<=5000 vs amount>50000 槽位隔离，彻底防串分支)
                 │
      [核验组织架构快照] ── (核验申请人部门、领导是否发生离职/调岗)
         ├── 快照吻合且在 7天 TTL 内 ──► ⚡ [0 Token 毫秒级极速命中] (零成本 / 完全免疫外部LLM宕机)
         └── 发生人员变更或新规则   ──► 🤖 [LLM 智能解析与指派] ──► 自动更新快照
```

---

### 2. LLM 供应商宕机自愈防护体系 (Outage Resilience)

当 DeepSeek/OpenAI 等外部大模型发生 **502 网关错误、请求超时、429 限流或欠费** 时，Rillway 启动四层自愈机制，**确保业务绝对不中断**：

```text
1. 决策快照命中 ──► 0 Token 读取本地缓存，根本不发网络请求，完全免疫宕机；
2. 本地语义降级 ──► 从 Prompt 提取 "直属领导/部门主管/总经理"，自动调 IdentityService 顺延派发；
3. 人员空缺顺延 ──► 领导离职或空缺时，自动向上顺延至部门负责人或 admin 兜底；
4. AI 审批安全降级 ──► Agent 自动合规审批失败时，安全转交人工复核待办，绝不越权盲批。
```

---

### 3. 大模型与方法调用全链路追溯审计 (`rillway_ai_trace`)

#### Profile 开关配置 (`application.yml`)：
```yaml
rillway:
  ai:
    trace:
      enabled: true          # 大模型与方法调用追溯总开关（默认 true，支持按 Profile 关闭）
      log-payload: true      # 记录完整 Prompt 提示词与 Response 响应报文
      record-tool-calls: true # 记录触发的 Java 方法名、入参 JSON 与执行返回值
```

#### 自动初始化审计表：
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `trace_id` | VARCHAR(64) | 链路 ID（关联流程实例 ID） |
| `model` | VARCHAR(64) | 调用的模型名称（如 `deepseek-chat`） |
| `call_type` | VARCHAR(32) | 调用类型：`CHAT` / `CONTINUE_CHAT` / `OUTAGE_FALLBACK` |
| `prompt_text` / `response_text` | TEXT | 请求完整 Prompt 提示词与响应结果 |
| `tool_name` / `tool_arguments` / `tool_result` | TEXT | 触发的 Java 方法名、入参 JSON 与执行结果 |
| `total_tokens` / `latency_ms` | INT / BIGINT | Token 消耗统计与调用耗时（毫秒） |
| `status` / `error_message` | VARCHAR / TEXT | 状态（`SUCCESS`/`FAILED`/`DEGRADED`）与异常堆栈 |

---

### 4. 零代码单据状态回写机制 (Entity Status Auto-Update)

#### 痛点对比：传统工作流 vs Rillway
| 对比维度 | 传统工作流 (Flowable / Activiti) | ⚡ Rillway AI 原生引擎 |
| :--- | :--- | :--- |
| **状态同步方式** | 必须编写 `JavaDelegate` / `ExecutionListener` 注入业务 Mapper 手动调用 `updateById()` | **零代码配置驱动**，配置表指定 `status_column` 即可 |
| **事务一致性** | 流程引擎库与业务库分离，极易产生**流程走完但业务单据状态未更新**的分布式事务脏数据 | **本地事务原子绑定**，共享 Spring `DataSourceTransactionManager`，同生共死 |
| **代码侵入性** | 业务代码与流程 API 强耦合，充斥大量 `runtimeService` 与各种监听器模板代码 | **零侵入**，业务单据只有普通的 JavaBean，完全感知不到工作流框架存在 |

#### 🔄 全自动状态回写闭环链路：
```text
 1. 业务提交单据             2. 审批流转中               3. 终审通过 / 驳回
┌────────────────┐      ┌────────────────┐      ┌─────────────────────────┐
│ PurchaseOrder  │ ───► │  多级审批推进   │ ───► │ EntityStatusAutoUpdater │
│ status='DRAFT' │      │ status='UNDER_ │      │ 自动拦截 COMPLETED 事件  │
└────────────────┘      │    REVIEW'     │      └────────────┬────────────┘
                        └────────────────┘                   │
                                                             ▼
                                                自动执行精准 SQL 回写：
                                                UPDATE biz_purchase_order 
                                                SET status = 'APPROVED' 
                                                WHERE id = '100293847291';
```

#### ⚙️ 核心工作原理解析：
1. **自动元数据提取**：流程发起时，引擎从实体类注解（`@TableName` / `@TableId` 或 `@RillwayEntity`）提取出当前单据关联的表名 `biz_purchase_order` 和主键 ID `100293847291`；
2. **规则自动路由**：匹配 `rillway_binding_config` 表中预设的 `status_column`（状态列名）、`approved_value`（通过值）与 `rejected_value`（驳回值）；
3. **本地事务强一致保障**：当流程触发终审节点时，引擎通过 Spring `JdbcTemplate` 直接在**当前业务事务连接**内执行原子更新。若后续业务抛出异常，单据状态回写与流程实例状态一并回滚，彻底根除分布式事务一致性难题！

---

### 5. 领域事件监听机制 (Domain Event System)

Rillway 原生深度桥接 Spring 官方 `@EventListener` 生态，支持**全局通用监听（审计/日志）**与**基于 BusinessKey / 单据类型的精准条件过滤监听（下游业务联动）**：

#### ① 全局通用监听（适合：统一审计日志、全链路 APM 追踪、中台大屏）
```java
@Component
public class GlobalProcessAuditListener {

    // 🌟 全局监听所有流程的发起事件
    @EventListener
    public void onAnyProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("[全局审计] 流程启动: 实例ID={}, 单据Key={}, 发起人={}", 
                event.processInstanceId(), event.businessKey(), event.initiator());
    }

    // 🌟 全局监听所有流程的节点流转与审批决策
    @EventListener
    public void onAnyNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("[全局审计] 节点 [{}] 审批流转: 处理人={}, 决策={}, 意见={}", 
                event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }
}
```

#### ② 基于 BusinessKey / 单据类型的精准过滤监听（适合：特定单据开单、发邮件）
通过 Spring SpEL 表达式 `condition`，**无需手写 if-else 过滤**，只处理目标业务单据：

```java
@Component
public class PurchaseOrderEventListener {

    // 🎯 仅监听采购单 (businessKey 以 'biz_purchase_order' 开头) 且【终审通过】的事件
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('biz_purchase_order') && #event.isSuccess")
    public void onPurchaseOrderApproved(ProcessEvent.ProcessCompletedEvent event) {
        String orderId = event.businessKey().replace("biz_purchase_order:", "");
        log.info("🎉 采购单 [{}] 终审批准通过，自动调用 ERP 创建采购入库单！", orderId);
        erpService.createPurchaseReceipt(orderId);
    }

    // 🎯 仅监听采购单【审批驳回】事件 -> 通知申请人
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('biz_purchase_order') && !#event.isSuccess")
    public void onPurchaseOrderRejected(ProcessEvent.ProcessCompletedEvent event) {
        log.warn("❌ 采购单 [{}] 已被驳回，推送企业微信通知申请人！", event.businessKey());
        wechatNotifyService.sendRejectAlert(event.businessKey());
    }
}
```

---

### 6. 企业级边界与运维处理

- **终审节点感知**：
  ```java
  // 探测当前待办是否为终审关卡（直接连向 EndNode，前端可提示“您是终审人”）
  boolean isFinal = taskService.isTerminalTask(taskId);
  ```
- **离职调岗一键转办**：
  ```java
  // 原处理人离职，管理员一键转派新接任者，原人员待办自动移除
  taskService.transferTask(taskId, "New_Manager_Charlie", "原主管离职转办");
  ```
- **直属领导空缺顺延**：
  ```java
  // 领导离职或为空时，自动向上寻根至部门负责人或管理员兜底
  Optional<String> leader = identityService.getEffectiveDirectLeader(userId);
  ```

### 7. 基于实体 Class 的 DDL Schema 自省与大模型 Tool Calling 编译器

#### 架构工作流程：
```text
 1. 注册实体 Class        2. 自动反射与 DDL 注解自省            3. 赋能大模型 Tool Calling
┌─────────────────┐      ┌─────────────────────────┐      ┌────────────────────────┐
│ SystemUserDO    │ ───► │ EntityClassIntrospector │ ───► │ buildWorkflowDag       │
│ SystemDeptDO    │      │ 自动提取表名、列名、注释  │      │ 大模型输出强类型 DAG    │
│ SystemRoleDO    │      │ 及部门外键、领导列关联    │      │ START -> RULE -> HUMAN │
└─────────────────┘      └─────────────────────────┘      └────────────────────────┘
```

#### 特性优势：
1. **开发者零代码成本**：无需写任何复杂的查询接口，直接传 Class；
2. **拒绝伪流程硬编码**：大模型通过标准 OpenAI Tool Calling 接口调用 `buildWorkflowDag`，动态根据业务单据（`day`, `amount`, `type`）与组织架构（`deptName`, `leaderUserId`, `roleCode`）输出包含通用条件表达式和拓扑连线的闭环 DAG 图；
3. **前端零门槛渲染**：生成的 `ProcessDefinition` 可直接被前端流程设计器或 `ProcessPreviewer` 进行路径预测展示与节点定位。

---

## 🗄️ 数据库核心表清单 (全部自动建表 Auto-DDL)

应用启动时自动检测连接池并初始化，**零 DDL 维护成本**：
1. `rillway_instance`：流程实例运行时状态与上下文快照表
2. `rillway_task`：全员人工待办与已办任务中心表
3. `rillway_history`：流程节点全生命周期流转历史轨迹与审计事件表
4. `rillway_binding_config`：零代码业务单据与自然语言制度绑定配置表（支持一键大模型生成 DAG）
5. `rillway_resolution_cache`：大模型审批决策快照与分支隔离缓存表
6. `rillway_ai_config`：大模型连接与 API Key 热插拔配置表（支持 DeepSeek, OpenAI, 智谱, Qwen, Ollama）
7. `rillway_ai_trace`：大模型与 Tool Calling 全链路追溯审计日志表

---

## 🌐 示例工程 RESTful 控制台 API 清单 (`rillway-example`)

| 模块 / 底表 | 方法 | API 端点 | 功能说明 |
| :--- | :--- | :--- | :--- |
| **Prompt 编译器** | `POST` | `/api/workflow/compile` | 传入任意自然语言审批制度，大模型即时编译并返回标准 DAG 图 |
| **流程预测预览** | `POST` | `/api/hrm/leave/preview` | 员工提交表单前，预测流经节点路径、审批人及 AI 智能审查节点 |
| **实体流程发起** | `POST` | `/api/hrm/leave/apply` | 员工提交请假申请，引擎自动匹配规则并生成多级待办 |
| **待办任务办理** | `POST` | `/api/workflow/tasks/{id}/approve` | 审批人提交审批通过意见，自动推进至下一关卡或终审完成 |
| **单据规则绑定** | `GET/POST`| `/api/admin/binding-config` | 查询或保存单据与表字段映射，配置 `process_prompt` 制度 |
| **一键生成流程** | `POST` | `/api/admin/binding-config/{id}/generate-flow` | **核心亮点**：根据配置表的自然语言制度，一键调用大模型编译生成流程定义并绑定 |
| **大模型热插拔** | `GET/POST`| `/api/admin/ai-config` | 动态热切换大模型连接、Base URL 与 API Key，无需重启服务 |
| **决策快照缓存** | `GET/DEL` | `/api/admin/resolution-cache` | 查看大模型决策分支缓存快照，或一键按流程/全局清空缓存 |
| **AI 调用全链路追溯** | `GET` | `/api/admin/ai-trace` | 实时查看大模型 Prompt、Tool Calling 入参、响应与消耗 Token 统计 |
| **全员任务监控** | `GET/POST`| `/api/admin/tasks` | 管理员监控全员任务，支持强制委派重派 (`/reassign`) |
| **实例运行时管理** | `GET/POST`| `/api/admin/instances` | 流程实例运行监控，支持管理员后台强制终止作废 (`/terminate`) |

---

## 📄 开源协议

本项目基于 [Apache 2.0 License](LICENSE) 开源。
