<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3+-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/Architecture-AI--Native-6366F1?style=for-the-badge" alt="AI Native" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge" alt="License" />
</p>

<h1 align="center">⚡ Rillway: AI-Native Workflow Engine for Modern Middle Platforms</h1>

<p align="center">
  <b>Say goodbye to verbose XML and heavy BPMN. Describe approval policies in plain natural language, launch workflows directly by passing domain entity beans, update entity status with zero code, cache decisions at 0-Token cost, and achieve enterprise-grade resilience against LLM outages.</b>
</p>

<p align="center">
  <a href="README.md"><b>简体中文</b></a> | <a href="README_EN.md"><b>English</b></a>
</p>

---

## 💡 Why Rillway?

Traditional workflow engines (Activiti, Flowable, Camunda) introduce friction in modern Spring Boot ecosystems and LLM adoption:
- **High integration friction**: Requires drawing complex BPMN XMLs and manually calling `variables.put()` for each entity field;
- **Heavy business coupling**: Entity status synchronization requires verbose Listeners/Delegates and easily breaks local database transactions;
- **LLM adoption bottlenecks**: External LLMs suffer from network timeouts/outages and high Token costs, lacking organizational snapshot verification and decision cache mechanisms.

**Rillway is purpose-built to solve these challenges:**
1. **Natural Language Policy Definition**: Describe approval policies in plain language without drawing BPMN flowcharts;
2. **Direct Entity Bean Invocation**: Start workflows with a single line of code by passing business Beans; automatically extracts table name, primary key, initiator, and masks sensitive data;
3. **Zero-Code Entity Status Binding**: Config-driven automatic SQL updates on approval/rejection, sharing local `@Transactional` boundaries;
4. **0-Token Decision Cache**: Automatically verifies organizational snapshots and isolates condition branches with millisecond-level cache hits;
5. **LLM Outage Self-Healing**: Automatically degrades to semantic escalation and safe human review when LLM APIs return 502/timeout, ensuring business continuity;
6. **Full-Link LLM & Tool Invocation Trace**: Auto-creates `rillway_ai_trace` table to audit prompts, responses, latency, tokens, and Java method arguments.

---

## 🚀 3-Minute End-to-End Quick Start

Integrate an AI-driven approval workflow in just **3 simple steps**:

### Step 1: Add Maven Starter
```xml
<dependency>
    <groupId>com.wegongdu.rillway</groupId>
    <artifactId>rillway-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Configure LLM & Tracing (`application.yml`)
Configure your OpenAI-compatible LLM endpoint (DeepSeek, Qwen, Azure OpenAI, Ollama, etc.):

```yaml
rillway:
  enabled: true
  ai:
    openai:
      enabled: true
      base-url: https://api.deepseek.com/v1   # Or DashScope / Azure endpoint
      api-key: sk-your-api-key-here           # Your API Key
      model: deepseek-chat                    # Model name (e.g. deepseek-chat, qwen-plus)
      temperature: 0.1
      timeout-seconds: 30
    trace:
      enabled: true                           # 🌟 Master switch for LLM & Tool tracing
      log-payload: true                       # Logs full prompt & response payloads
      record-tool-calls: true                 # Logs Java tool names, argument JSONs & outputs
```
> 💡 **Tip**: You can also insert configs directly into database table `rillway_ai_config` for zero-deployment hot swapping.

### Step 3: Configure Natural Language Policy & Status Binding (`rillway_binding_config`)
The table is automatically initialized on application startup. Insert your **natural language policy** and **table binding**:

```sql
INSERT INTO rillway_binding_config (
    id, 
    business_type,      -- Business type or table name to bind (e.g. biz_purchase_order)
    process_prompt,     -- 🌟 Natural language policy: LLM dynamically interprets and executes routing
    table_name,         -- Bound business table name
    status_column,      -- Status column to update automatically
    approved_value,     -- Status value on approval (e.g. APPROVED)
    rejected_value,     -- Status value on rejection (e.g. REJECTED)
    enabled
) VALUES (
    'cfg_purchase_01',
    'biz_purchase_order',
    'Employee submits purchase request:
     1. When amount < 5,000, direct department manager approves;
     2. When amount between 5,000 and 50,000 with compliant invoice, AI Compliance Agent auto-approves;
     3. When amount > 50,000 or missing invoice, escalate to Department Director & General Manager.',
    'biz_purchase_order',
    'status',
    'APPROVED',
    'REJECTED',
    true
);
```

### Step 4: Define Domain Bean & Launch Workflow

#### ① Domain Entity Definition (Reuse Domain Bean + Annotation Extensions)
```java
@Data
@TableName("biz_purchase_order") // 🌟 Compatible with MyBatis-Plus @TableName / JPA @Table
@RillwayEntity(businessType = "biz_purchase_order") // Optional: explicitly declare business type
public class PurchaseOrder {

    @TableId // 🌟 Compatible with MyBatis-Plus @TableId, JPA @Id, or Rillway @EntityId
    private Long id; // Supports Long Snowflake ID or String business key

    @ProcessInitiator // 🌟 Declares workflow initiator (supports Long userId or String username)
    private String applicant;

    private BigDecimal amount;       // Auto-mapped to context.variable("amount", 12000)
    private Boolean hasInvoice;      // Auto-mapped to context.variable("hasInvoice", true)

    @ProcessVariable("dept_code")    // Optional: customize workflow variable key name
    private String departmentCode;

    @ProcessIgnore                   // 🌟 Auto-masks sensitive fields (passwords, secrets, byte arrays)
    private String internalRemark;
}
```

#### ② Start Workflow with One Line of Code
```java
@Autowired
private ProcessEngine processEngine;

// 🚀 Just pass the entity Bean!
// Rillway automatically resolves the matching policy, extracts variables, runs the flow,
// and updates the record status to 'APPROVED' upon completion!
ProcessInstance instance = processEngine.start(purchaseOrder);
```

---

## 🎯 Key Technical Mechanisms

### 1. 0-Token Decision Snapshot Cache (ResolutionCache)

To resolve the challenges of **slow latency (seconds), high Token cost, and network flakiness**, Rillway features a condition-branch-isolated decision cache:

```text
          [Submit Domain Entity]
                    │
   [Extract Condition Branch Key] ── (e.g. amount<=5000 vs amount>50000 slot isolation)
                    │
   [Verify Org Snapshot & TTL]   ── (Has applicant's dept or leader changed?)
         ├── Snapshot Matches (TTL 7d) ──► ⚡ [0 Token Fast-Path] (Zero cost / immune to LLM outages)
         └── Org Changed / First Time  ──► 🤖 [LLM Tool Calling] ──► Auto-Update Snapshot
```

---

### 2. Outage Resilience & Self-Healing Architecture

When external LLMs encounter **502 Gateway Errors, Read Timeouts, 429 Rate Limits, or Outages**, Rillway triggers a 4-layer resilience chain:

```text
1. Decision Cache Hit ──► Reads cached snapshot with 0 Token cost; 100% immune to outages;
2. Deterministic Fallback ──► Extracts "direct leader / dept manager / GM" from prompt and calls IdentityService;
3. Org Hierarchy Escalation ──► If leader is missing or offboarded, escalates up the management chain to admin;
4. Agent Authority Fallback ──► If an autonomous compliance Agent fails, degrades safely to Human Task for manual review.
```

---

### 3. Full-Link LLM & Tool Invocation Trace (`rillway_ai_trace`)

#### Profile Configuration (`application.yml`):
```yaml
rillway:
  ai:
    trace:
      enabled: true          # Master switch for LLM and Tool tracing (Default: true)
      log-payload: true      # Logs complete prompt and response payloads
      record-tool-calls: true # Logs triggered Java tool names, arguments JSON, and return values
```

#### Auto-Created Audit Table Schema:
| Column | Type | Description |
| :--- | :--- | :--- |
| `trace_id` | VARCHAR(64) | Correlation ID (linked to process instance ID) |
| `model` | VARCHAR(64) | Model name invoked (e.g. `deepseek-chat`, `gpt-4o`) |
| `call_type` | VARCHAR(32) | Call type: `CHAT` / `CONTINUE_CHAT` / `OUTAGE_FALLBACK` |
| `prompt_text` / `response_text` | TEXT | Raw prompt text and LLM output markdown |
| `tool_name` / `tool_arguments` / `tool_result` | TEXT | Executed Java tool method, input JSON, and returned output |
| `total_tokens` / `latency_ms` | INT / BIGINT | Token usage statistics and latency in milliseconds |
| `status` / `error_message` | VARCHAR / TEXT | Execution status (`SUCCESS`/`FAILED`/`DEGRADED`) and error stack |

---

### 4. Zero-Code Entity Status Auto-Update (EntityStatusAutoUpdater)

Eliminate boilerplate MyBatis / JPA Update statements. Upon task approvals or rejections, Rillway automatically triggers `EntityStatusAutoUpdater` to update the bound table:

```sql
-- Automatically executed by Rillway upon approval:
UPDATE biz_purchase_order 
SET status = 'APPROVED' 
WHERE id = 'PO_20260818_001';
```

- **Local Transaction Atomicity (Zero-Distributed-Tx)**:
  Shares the same Spring `@Transactional` local database connection. Domain record insertion, workflow state transition, and status column updates are committed or rolled back atomically within the **same local transaction** without external distributed transaction coordinators (e.g. Seata).
- **Graceful Fallback**: Automatically degrades to in-memory simulation in non-database environments (e.g. unit tests).

---

### 5. Domain Event Listener System (Spring Event Bridge)

Rillway natively bridges workflow lifecycle events directly into Spring's standard `@EventListener` ecosystem, allowing seamless, decoupled integration with downstream systems (DingTalk/Slack notifications, email alerts, ERP purchase order creation):

```java
@Component
public class PurchaseWorkflowEventListener {

    // 1. Listen for process initiation
    @EventListener
    public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("Workflow started: instanceId={}, businessKey={}, initiator={}", 
                event.processInstanceId(), event.businessKey(), event.initiator());
    }

    // 2. Listen for node decision completion
    @EventListener
    public void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("Node [{}] completed: actor={}, decision={}, reason={}", 
                event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }

    // 3. Listen for final process completion / archiving (trigger ERP receipts)
    @EventListener
    public void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
        if (event.isSuccess()) {
            log.info("🎉 Order [{}] approved! Triggering ERP receipt creation...", event.businessKey());
            erpService.createPurchaseReceipt(event.businessKey());
        } else {
            log.warn("❌ Order [{}] rejected. Notifying applicant...", event.businessKey());
            notifyService.sendRejectNotice(event.businessKey());
        }
    }
}
```

---

### 6. Enterprise Edge Cases & Operations

- **Terminal Node Detection**:
  ```java
  // Detects whether the current task is the final approval step before completion
  boolean isFinal = taskService.isTerminalTask(taskId);
  ```
- **Employee Offboarding / Task Reassignment**:
  ```java
  // Reassigns tasks when an employee leaves or transfers; instantly removed from old user's queue
  taskService.transferTask(taskId, "New_Manager_Charlie", "Manager Bob offboarded");
  ```
- **Leader Missing / Escalation**:
  ```java
  // Escalates to department manager or admin when direct leader is missing
  Optional<String> leader = identityService.getEffectiveDirectLeader(userId);
  ```

---

## 🗄️ Core Tables (Zero-Config Auto-DDL)

Auto-initialized on application startup when a DataSource is detected:
1. `rillway_instance`: Process instances and context snapshots
2. `rillway_task`: Human task inbox and candidate tasks
3. `rillway_history`: Execution history records and audit events
4. `rillway_binding_config`: Zero-code business entity and natural language policy binding rules
5. `rillway_resolution_cache`: Decision snapshots and branch-isolated caches
6. `rillway_ai_config`: LLM connection and API Key hot-swap configs
7. `rillway_ai_trace`: Full-link LLM and Tool Calling audit logs

---

## 📄 License

Rillway is licensed under the [Apache 2.0 License](LICENSE).
