# ⚙️ Halleyx Workflow Engine

> Advanced Workflow Engine — Halleyx Full Stack Engineer Challenge I - 2026

A production-grade workflow execution system that lets users **design workflows**, **define rules**, **execute processes**, and **track every step** with a dynamic rule engine, REST API, and built-in dashboard UI.

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run (H2 in-memory DB — zero config)
```bash
mvn spring-boot:run
```

### Access
| URL | Description |
|-----|-------------|
| http://localhost:8080 | 🖥️ Workflow Dashboard UI |
| http://localhost:8080/swagger-ui.html | 📖 Swagger API Docs |
| http://localhost:8080/h2-console | 🗃️ H2 Database Console |

---

## 🏗️ Architecture

```
halleyx-workflow/
├── engine/
│   └── RuleEngine.java          # SpEL-based rule evaluator
├── entity/
│   ├── Workflow.java             # Workflow with versioning
│   ├── Step.java                 # Task / Approval / Notification
│   ├── Rule.java                 # Conditional routing rules
│   ├── Execution.java            # Runtime execution record
│   └── StepLog.java              # Per-step audit logs
├── service/
│   ├── WorkflowService.java
│   ├── StepService.java
│   ├── RuleService.java
│   └── ExecutionService.java     # Workflow runner with loop protection
├── controller/
│   ├── WorkflowController.java
│   ├── StepController.java
│   ├── RuleController.java
│   └── ExecutionController.java
├── config/
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── DataInitializer.java      # Seeds 2 sample workflows
└── resources/
    ├── application.yml
    └── static/index.html         # Full dashboard UI
```

---

## 📋 Core Concepts

### Workflow
A process with versioned steps. Version auto-increments on every update.

### Step Types
| Type | Description |
|------|-------------|
| `TASK` | Automated action (DB update, report generation) |
| `APPROVAL` | Requires user sign-off (`assignee_email` in metadata) |
| `NOTIFICATION` | Sends alert via channel (`email`, `slack`) |

### Rule Engine
Rules are evaluated **in priority order** (lowest number = highest priority). Supports:
- Comparison: `==`, `!=`, `>`, `<`, `>=`, `<=`
- Logical: `&&`, `||`
- String: `contains(field, "value")`, `startsWith()`, `endsWith()`
- `DEFAULT` — fallback when no rule matches

**Example rules for Manager Approval step:**
| Priority | Condition | Next Step |
|----------|-----------|-----------|
| 1 | `amount > 100 && country == 'US' && priority == 'High'` | Finance Notification |
| 2 | `amount <= 100` | Task Completion |
| 3 | `priority == 'Low' && country != 'US'` | Task Rejection |
| 4 | `DEFAULT` | Task Rejection |

---

## 🔌 REST API

### Workflows
```
POST   /api/v1/workflows           # Create
GET    /api/v1/workflows           # List (search, filter, paginate)
GET    /api/v1/workflows/:id       # Get with steps & rules
PUT    /api/v1/workflows/:id       # Update (version++)
DELETE /api/v1/workflows/:id       # Delete
```

### Steps
```
POST   /api/v1/workflows/:id/steps  # Add step
GET    /api/v1/workflows/:id/steps  # List steps
PUT    /api/v1/steps/:id            # Update step
DELETE /api/v1/steps/:id            # Delete step
```

### Rules
```
POST   /api/v1/steps/:id/rules     # Add rule
GET    /api/v1/steps/:id/rules     # List rules
PUT    /api/v1/rules/:id           # Update rule
DELETE /api/v1/rules/:id           # Delete rule
```

### Execution
```
POST   /api/v1/workflows/:id/execute   # Execute workflow
GET    /api/v1/executions              # List executions
GET    /api/v1/executions/:id          # Get execution + logs
POST   /api/v1/executions/:id/cancel   # Cancel
POST   /api/v1/executions/:id/retry    # Retry failed step
```

---

## 🎯 Sample Workflows (Auto-seeded)

### 1. Expense Approval
```
Input: { "amount": 500, "country": "US", "priority": "High" }
Flow: Manager Approval → Finance Notification → CEO Approval → Task Completion
```

### 2. Employee Onboarding
```
Input: { "department": "Engineering", "role": "Backend" }
Flow: Account Setup → HR Approval → Welcome Notification
```

---

## 🛡️ Advanced Features
- **Workflow Versioning** — every update creates a new version
- **Loop Protection** — configurable max iterations (default: 50)
- **Input Schema Validation** — validates required fields before execution
- **Step Execution Logs** — duration, rule evaluated, next step, approver
- **Retry/Cancel** — manage failed or running executions
- **Pagination & Search** — on all list endpoints
- **Swagger UI** — full interactive API docs
- **H2 + PostgreSQL** — switch with `spring.profiles.active=postgres`

---

## 🗃️ PostgreSQL Setup

```yaml
# application.yml
spring:
  profiles:
    active: postgres
  datasource:
    url: jdbc:postgresql://localhost:5432/halleyx_workflow
    username: postgres
    password: password
```

```sql
CREATE DATABASE halleyx_workflow;
```

Then: `mvn spring-boot:run -Dspring.profiles.active=postgres`

---

## 🧪 Sample Execution

```bash
# 1. Create workflow
POST /api/v1/workflows
{ "name": "Expense Approval", "inputSchema": "{\"required\":[\"amount\",\"country\"]}" }

# 2. Execute with input
POST /api/v1/workflows/{id}/execute
{ "inputData": { "amount": 500, "country": "US", "priority": "High" }, "triggeredBy": "alice@co.com" }

# 3. View execution logs
GET /api/v1/executions/{executionId}
```

---

*Built for Halleyx Full Stack Engineer Challenge I — 2026*
