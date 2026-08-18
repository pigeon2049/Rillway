<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3+-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot 3" />
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

## 🚀 3 分钟端到端快速上手

只需要简单的 **3 步**，即可完成全套 AI 审批流程接入：

### 第 1 步：引入 Maven Starter
```xml
<dependency>
    <groupId>com.wegongdu.rillway</groupId>
    <artifactId>rillway-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 第 2 步：配置大模型与追踪参数 (`application.yml`)
在配置文件中填入大模型连接信息（天然兼容 DeepSeek、通义千问、OpenAI、Ollama 等任何 OpenAI 兼容协议接口）：

```yaml
rillway:
  enabled: true
  ai:
    openai:
      enabled: true
      base-url: https://api.deepseek.com/v1   # 或 https://dashscope.aliyuncs.com/compatible-mode/v1 等
      api-key: sk-your-api-key-here           # 你的 API Key
      model: deepseek-chat                    # 模型名称 (如 deepseek-chat, qwen-plus, gpt-4o-mini)
      temperature: 0.1
      timeout-seconds: 30
    trace:
      enabled: true                           # 🌟 开启大模型与方法调用全链路追溯审计
      log-payload: true                       # 记录完整 Prompt 与 Response 报文
      record-tool-calls: true                 # 记录触发的 Java 方法名、入参 JSON 与返回值
```
> 💡 **提示**：也可以不写在 yml 里，直接向数据库表 `rillway_ai_config` 插入记录实现热插拔免重启。

### 第 3 步：配置自然语言审批制度与状态回写 (`rillway_binding_config`)
应用启动会自动初始化该表。向表中配置你的**自然语言制度**与**绑定的单据表名**：

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

### 第 4 步：定义业务实体 Bean 并直接发起流程

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

### 4. 业务表状态字段自动回写机制 (Entity Status Auto-Update)

无需手写任何 MyBatis / JPA Update 代码，在流程流转、审批通过或驳回时，Rillway 自动触发 `EntityStatusAutoUpdater` 执行精准的 SQL 回写：

```sql
-- 流程审批通过时，底层自动执行：
UPDATE biz_purchase_order 
SET status = 'APPROVED' 
WHERE id = 'PO_20260818_001';
```

- **事务同生共死 (Atomic & Zero-Distributed-Tx)**：
  底层基于同一个 Spring `@Transactional` 本地事务与数据库连接，业务单据插入、流程实例流转与状态字段回写在**同一本地事务中提交或回滚**，彻底告别分布式事务框架（如 Seata）的运维与性能负担！
- **自适应降级**：无数据库环境自动转为内存模拟，本地单测开箱即跑。

---

### 5. 领域事件监听机制 (Domain Event System)

Rillway 提供了双模事件发布机制，深度融合 Spring 官方 `@EventListener` 生态。业务系统可零侵入监听流程各阶段生命周期，实现下游联动（如发钉钉/企业微信、邮件通知、ERP 自动开单）：

```java
@Component
public class PurchaseWorkflowEventListener {

    // 1. 监听流程发起事件
    @EventListener
    public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("流程已启动: 实例ID={}, 单据Key={}, 发起人={}", 
                event.processInstanceId(), event.businessKey(), event.initiator());
    }

    // 2. 监听审批节点完成事件（记录决策）
    @EventListener
    public void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("节点 [{}] 审批完成: 处理人={}, 决策={}, 意见={}", 
                event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }

    // 3. 监听流程终审归档事件（触发下游 ERP / 发邮件）
    @EventListener
    public void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
        if (event.isSuccess()) {
            log.info("🎉 采购单 [{}] 终审通过，自动调用 ERP 创建采购入库单！", event.businessKey());
            erpService.createPurchaseReceipt(event.businessKey());
        } else {
            log.warn("❌ 采购单 [{}] 已被驳回，通知申请人修正！", event.businessKey());
            notifyService.sendRejectNotice(event.businessKey());
        }
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

---

## 🗄️ 数据库核心表清单 (全部自动建表 Auto-DDL)

应用启动时自动检测连接池并初始化，**零 DDL 维护成本**：
1. `rillway_instance`：流程实例与上下文快照
2. `rillway_task`：待办与已办任务中心
3. `rillway_history`：执行历史轨迹与审计事件
4. `rillway_binding_config`：零代码业务单据与自然语言制度绑定表
5. `rillway_resolution_cache`：大模型决策快照与分支缓存表
6. `rillway_ai_config`：大模型连接与 API Key 热插拔配置表
7. `rillway_ai_trace`：大模型与 Tool Calling 方法调用全链路追溯审计表

---

## 📄 开源协议

本项目基于 [Apache 2.0 License](LICENSE) 开源。
