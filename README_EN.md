<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3+-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot 3" />
  <a href="https://jitpack.io/#pigeon2049/Rillway"><img src="https://img.shields.io/badge/JitPack-v0.1.0-brightgreen?style=for-the-badge&logo=apachemaven" alt="JitPack" /></a>
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

## 🚀 Step-by-Step Integration (5-Step Quick Start)

Follow standard software engineering practices to get an AI workflow running in **5 simple steps**:

### Step 1: Add Maven Dependency (via JitPack)

Add the JitPack repository and Starter dependency to your project's `pom.xml`:

```xml
<!-- 1. Add JitPack Repository -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- 2. Add Rillway Spring Boot Starter -->
<dependencies>
    <dependency>
        <groupId>com.github.pigeon2049.Rillway</groupId>
        <artifactId>rillway-spring-boot-starter</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

---

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

---

### Step 3: Connect Enterprise Organization & Personnel (Two Options)

#### 🌟 Option A (Recommended · Zero-Code): Directly Register Entity Classes (`OrgEntityRegistry`)

**Zero custom SQL, Mappers, or Service implementations required!** Simply register your existing User, Department, Role, or Post entity classes as a Spring Bean. Rillway's **reflective DDL Introspector (`EntityClassIntrospector`)** automatically extracts table names, column mappings, foreign keys, and Swagger/OpenAPI `@Schema` annotations, feeding them directly to the LLM for tool-driven evaluation:

```java
@Configuration
public class WorkflowOrgConfig {

    @Bean
    public OrgEntityRegistry orgEntityRegistry() {
        return OrgEntityRegistry.builder()
                .userEntity(SystemUserDO.class)      // User / Employee Entity
                .deptEntity(SystemDeptDO.class)      // Department Entity
                .roleEntity(SystemRoleDO.class)      // Role Entity
                .postEntity(SystemPostDO.class)      // Post / Position Entity (Optional)
                .build();
    }
}
```

---

#### Option B: Implement `IdentityService` SPI Interface (For Remote Microservice RPC)

If your system queries a centralized user center via Feign / gRPC / RPC, you can implement the `IdentityService` interface:

```java
@Service
public class EnterpriseIdentityService implements IdentityService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptMapper deptMapper;

    // 1. Get complete user profile (department, job position, leader ID)
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

    // 2. Query applicant's direct manager user ID
    @Override
    public Optional<String> getDirectLeader(String userId) {
        return Optional.ofNullable(userMapper.selectLeaderIdByUserId(userId));
    }

    // 3. Query department manager / director ID
    @Override
    public Optional<String> getDepartmentManager(String departmentId) {
        return Optional.ofNullable(deptMapper.selectLeaderIdByDeptId(departmentId));
    }

    // 4. Query user IDs by role or post (e.g. 'ROLE_HRBP')
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
> 💡 **Out-of-the-Box Fallback**: If neither option is configured, Rillway automatically uses `DefaultIdentityService` for painless unit testing and local development.

---

### Step 4: Configure Natural Language Policy & Status Binding (`rillway_binding_config`)

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

---

### Step 5: Define Domain Bean & Launch Workflow

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

#### Pain Point Comparison: Traditional Engines vs Rillway
| Metric | Traditional Engines (Flowable / Activiti) | ⚡ Rillway AI-Native Engine |
| :--- | :--- | :--- |
| **Status Synchronization** | Must write verbose `JavaDelegate` / `ExecutionListener` injecting business Mappers to call `updateById()` | **Zero-code config driven**—just declare `status_column` in config |
| **Transactional Consistency** | Separate databases between engine and business entities; prone to dirty data where workflows complete but entities fail to update | **Local Transaction Bound**—shares Spring `DataSourceTransactionManager`, commits and rolls back atomically |
| **Code Intrusion** | Business code is heavily coupled with workflow APIs, flooded with `runtimeService` and listener boilerplates | **Zero Intrusion**—domain records remain clean POJOs/JavaBeans with zero workflow framework dependencies |

#### 🔄 Automated Status Synchronization Lifecycle:
```text
 1. Submit Entity            2. Multi-Stage Review          3. Final Approval / Rejection
┌────────────────┐      ┌────────────────────┐      ┌─────────────────────────┐
│ PurchaseOrder  │ ───► │ Workflow Execution │ ───► │ EntityStatusAutoUpdater │
│ status='DRAFT' │      │ status='UNDER_     │      │ Intercepts COMPLETED    │
└────────────────┘      │    REVIEW'         │      └────────────┬────────────┘
                        └────────────────────┘                   │
                                                                 ▼
                                                    Executes Precise SQL Update:
                                                    UPDATE biz_purchase_order 
                                                    SET status = 'APPROVED' 
                                                    WHERE id = '100293847291';
```

#### ⚙️ How It Works:
1. **Automatic Metadata Extraction**: Upon workflow initiation, Rillway extracts the bound table name (`biz_purchase_order`) and primary key ID (`100293847291`) from entity annotations (`@TableName`, `@TableId`, or `@RillwayEntity`);
2. **Rule Matching**: Matches pre-configured rules in `rillway_binding_config` (`status_column`, `approved_value`, `rejected_value`);
3. **Local Transaction Atomicity**: When the process reaches a terminal node, Rillway uses Spring's `JdbcTemplate` to execute the SQL update directly within the **active local business transaction connection**. If downstream business logic throws an error, both the workflow state and the entity status roll back together.

---

### 5. Domain Event Listener System (Spring Event Bridge)

Rillway natively bridges workflow lifecycle events directly into Spring's standard `@EventListener` ecosystem, supporting both **Global System-Wide Auditing** and **Targeted BusinessKey / Order-Type Filtering**:

#### ① Global Audit Listeners (Ideal for APM Tracing, Centralized Logs, BI Dashboards)
```java
@Component
public class GlobalProcessAuditListener {

    // 🌟 Listens to all workflow initiation events across the platform
    @EventListener
    public void onAnyProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("[Global Audit] Workflow started: instanceId={}, businessKey={}, initiator={}", 
                event.processInstanceId(), event.businessKey(), event.initiator());
    }

    // 🌟 Listens to all task transitions and decisions
    @EventListener
    public void onAnyNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("[Global Audit] Node [{}] completed: actor={}, decision={}, reason={}", 
                event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }
}
```

#### ② Targeted BusinessKey / Order-Type Listeners (Using Spring SpEL Conditions)
Zero boilerplate `if-else` checks—filter precisely using SpEL conditions:

```java
@Component
public class PurchaseOrderEventListener {

    // 🎯 Listens ONLY to purchase orders (businessKey starts with 'biz_purchase_order') that are APPROVED
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('biz_purchase_order') && #event.isSuccess")
    public void onPurchaseOrderApproved(ProcessEvent.ProcessCompletedEvent event) {
        String orderId = event.businessKey().replace("biz_purchase_order:", "");
        log.info("🎉 Purchase Order [{}] approved! Triggering ERP receipt creation...", orderId);
        erpService.createPurchaseReceipt(orderId);
    }

    // 🎯 Listens ONLY to purchase orders that are REJECTED -> alert applicant via WeChat/Slack
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('biz_purchase_order') && !#event.isSuccess")
    public void onPurchaseOrderRejected(ProcessEvent.ProcessCompletedEvent event) {
        log.warn("❌ Purchase Order [{}] rejected. Sending instant alert...", event.businessKey());
        notifyService.sendRejectAlert(event.businessKey());
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

### 7. Entity Class Schema Introspection & LLM Tool Calling DAG Compiler

#### Architecture Workflow:
```text
 1. Register Entity Classes   2. Reflective DDL Introspection      3. Empower LLM Tool Calling
┌─────────────────┐      ┌─────────────────────────┐      ┌────────────────────────┐
│ SystemUserDO    │ ───► │ EntityClassIntrospector │ ───► │ buildWorkflowDag       │
│ SystemDeptDO    │      │ Auto-extracts tables,   │      │ LLM outputs typed DAG  │
│ SystemRoleDO    │      │ columns, comments, FKs  │      │ START -> RULE -> HUMAN │
└─────────────────┘      └─────────────────────────┘      └────────────────────────┘
```

#### Key Advantages:
1. **Zero Coding Overhead**: No need to write repetitive CRUD queries or custom data mappers—just pass entity classes;
2. **Zero Hardcoding**: LLM calls the standard OpenAI Tool Calling interface `buildWorkflowDag`, dynamically generating clean, closed-loop DAG topologies based on business fields (`day`, `amount`, `type`) and organizational roles (`deptName`, `leaderUserId`, `roleCode`);
3. **Seamless Frontend Preview**: The generated `ProcessDefinition` DAG can be directly rendered in UI designers or dynamically predicted via `ProcessPreviewer`.

---

## 🗄️ Core Database Tables (Zero-Config Auto-DDL)

Auto-initialized on application startup when a DataSource is detected:
1. `rillway_instance`: Process instances runtime state and execution context snapshots
2. `rillway_task`: Human task inbox and assignee/candidate tasks
3. `rillway_history`: Execution history records and audit lifecycle events
4. `rillway_binding_config`: Zero-code business entity and natural language policy binding rules (supports one-click LLM DAG generation)
5. `rillway_resolution_cache`: Decision snapshots and branch-isolated caches
6. `rillway_ai_config`: LLM connection and API Key hot-swap configs (supports DeepSeek, OpenAI, GLM, Qwen, Ollama)
7. `rillway_ai_trace`: Full-link LLM and Tool Calling audit logs

---

## 🌐 Example Showcase RESTful Console APIs (`rillway-example`)

| Module / Table | Method | API Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Prompt Compiler** | `POST` | `/api/workflow/compile` | Compiles arbitrary natural language approval policies into standard DAG flowcharts |
| **Path Preview** | `POST` | `/api/hrm/leave/preview` | Predicts execution path, approver personnel, and AI Agent nodes before submission |
| **Entity Apply** | `POST` | `/api/hrm/leave/apply` | Initiates leave application entity and matches approval rules automatically |
| **Task Approve** | `POST` | `/api/workflow/tasks/{id}/approve` | Approver submits approval comment and advances flow to next stage or completion |
| **Binding Config** | `GET/POST`| `/api/admin/binding-config` | Queries or saves entity field bindings and `process_prompt` policy text |
| **One-Click Generate Flow** | `POST` | `/api/admin/binding-config/{id}/generate-flow` | **Key Feature**: Calls LLM to compile policy prompt from config table into a DAG workflow |
| **LLM Hot-Reload** | `GET/POST`| `/api/admin/ai-config` | Dynamically hot-swaps LLM connection, Base URL, and API Key without restarting service |
| **Decision Cache** | `GET/DEL` | `/api/admin/resolution-cache` | Views branch decision snapshots or clears cache globally / per process |
| **AI Audit Trace** | `GET` | `/api/admin/ai-trace` | Real-time audit logs of LLM Prompts, Tool Calling arguments, responses, and Token costs |
| **Task Operations** | `GET/POST`| `/api/admin/tasks` | Admin task inbox monitor, supports mandatory reassignment (`/reassign`) |
| **Instance Runtime** | `GET/POST`| `/api/admin/instances` | Runtime monitor with admin mandatory termination support (`/terminate`) |

---

## 📄 License

Rillway is licensed under the [Apache 2.0 License](LICENSE).
