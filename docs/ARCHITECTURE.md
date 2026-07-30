# Architecture Overview — Agentic URL Shortener

**Version**: 1.0.0 | **Status**: APPROVED | **Run ID**: wf-20260730-001

---

## 1. System Components

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          AGENTIC URL SHORTENER                               │
│                                                                              │
│  ┌──────────────┐    ┌──────────────────────────────────────────────────┐   │
│  │  FRONTEND    │    │                   BACKEND                         │   │
│  │              │    │                                                    │   │
│  │ Angular 19   │───▶│  UrlShortenerController  (POST /api/v1/urls)       │   │
│  │ Standalone   │    │  UrlShortenerController  (GET  /r/{code})          │   │
│  │ Components   │    │  UrlShortenerController  (GET  /api/v1/.../analytics│   │
│  │              │◀───│                                                    │   │
│  │ - Create URL │    │  ┌─────────────────────────────────────────────┐  │   │
│  │ - Analytics  │    │  │           UrlShortenerService               │  │   │
│  │   Dashboard  │    │  │  - validateDestination()                    │  │   │
│  └──────────────┘    │  │  - create(destinationUrl, expiresAt)        │  │   │
│                      │  │  - redirectAndRecordClick(shortUrl)         │  │   │
│   proxy.conf.json    │  │  - getAnalyticsCount(shortUrl)              │  │   │
│   /api → :8080       │  └──────────────┬──────────────────────────────┘  │   │
│   /r   → :8080       │                 │                                  │   │
│                      │  ┌──────────────▼──────────────────────────────┐  │   │
│                      │  │             JPA Repositories                 │  │   │
│                      │  │  ShortUrlRepository (findByCode, save)       │  │   │
│                      │  │  ClickAggregateRepository (findByBucket,     │  │   │
│                      │  │    sumClickCount)                            │  │   │
│                      │  └──────────────┬──────────────────────────────┘  │   │
│                      │                 │                                  │   │
│                      │  ┌──────────────▼──────────────────────────────┐  │   │
│                      │  │           H2 In-Memory Database              │  │   │
│                      │  │  short_url (code PK, destination_url,        │  │   │
│                      │  │            created_at, expires_at, status)   │  │   │
│                      │  │  click_aggregate (short_url_id FK,           │  │   │
│                      │  │            bucket_start, click_count)        │  │   │
│                      │  └──────────────────────────────────────────────┘  │   │
│                      └──────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    ORCHESTRATION CONTROL PLANE                        │   │
│  │                                                                        │   │
│  │  ┌──────────────────────┐    ┌──────────────────────────────────┐    │   │
│  │  │   WorkflowExecutor   │    │         ApprovalManager          │    │   │
│  │  │                      │◀──▶│                                  │    │   │
│  │  │  - DAG traversal     │    │  - isGateApproved(gate)          │    │   │
│  │  │  - dependency check  │    │  - approveGate(id, role, userId) │    │   │
│  │  │  - retry (max 3)     │    │  - getPendingApprovals()         │    │   │
│  │  │  - invalidate deps   │    │  - setApprover(role, userId)     │    │   │
│  │  └──────────┬───────────┘    └──────────────────────────────────┘    │   │
│  │             │                                                          │   │
│  │  ┌──────────▼──────────────────────────────────────────────────────┐ │   │
│  │  │                      Agent Registry                              │ │   │
│  │  │                                                                   │ │   │
│  │  │  RequirementsAnalysisAgent │ ImplementationAgent                 │ │   │
│  │  │  TestingAgent              │ DocumentationAgent                  │ │   │
│  │  └──────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                        │   │
│  │  WorkflowState (wf-20260730-001.json)                                  │   │
│  │  ├── nodes: 18 WorkflowNode entries with status + output              │   │
│  │  ├── approvals: 5 ApprovalRecord entries with role + timestamp        │   │
│  │  ├── transitions: complete audit trail                                │   │
│  │  └── retries / rollbacks                                              │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Orchestration Model

The orchestration layer implements a **governed, stateful DAG** (Directed Acyclic Graph). Nodes advance only when their declared dependencies have reached `SUCCEEDED` and any configured approval gate is cleared.

### Node Lifecycle

```
 PENDING ──── dependencies unmet ────────────────────┐
     │                                               │
     │ all dependencies SUCCEEDED                    │
     ▼                                               │
  READY ──── gate present + gate not approved ──▶ AWAITING_APPROVAL
     │                                               │
     │ gate approved (or no gate)                    │ gate approved later
     ▼                                               │
 RUNNING ◀──────────────────────────────────────────┘
     │
     ├──── success ──▶ SUCCEEDED
     │
     └──── error ──▶ retry < 3 ──▶ READY (back into queue)
                 └──── retry == 3 ──▶ FAILED ──▶ invalidate dependents (BLOCKED)
```

### 18-Node Workflow DAG

```
requirements ──────────────────────────────────────┐
    │                                               │
    ├──▶ ambiguity [gate: requirement_interpretation]│
    │                                               │
    ├──▶ decomposition                              │
    │         │                                     │
    │         └──▶ greenfield ◀──── api_schema ◀───┤
    │                   │            [gate: public_api_schema]
    │                   │                           │
    ├──▶ architecture [gate: architecture_adr] ─────┘
    │
    ├──▶ test_prep ──────────────┐
    │                            │
    └──▶ docs_prep               │
              │                  ▼
              │    greenfield ──▶ implementation
              │                        │
              │         ┌──────────────┼──────────────┐
              │         ▼              ▼              ▼
              │      testing   security [gate:   performance
              │                security_controls]
              │         │         │                   │
              │         └────┬────┘                   │
              │              │              reliability│
              │         brownfield                     │
              │              │                         │
              └──▶ documentation ◀────────────────────┘
                        │
                        ▼
               release_readiness [gate: production_readiness]
                        │
                        ▼
               governance_audit
                        │
                        ▼
               final_summary [gate: release_approval]
```

### Approval Gates (5 total)

| Gate | Guarding Node | Required Role | Purpose |
|------|--------------|---------------|---------|
| `requirement_interpretation` | ambiguity | product_owner | Approve ambiguity resolutions before design starts |
| `architecture_adr` | architecture | engineering_lead | Certify ADRs before implementation |
| `public_api_schema` | api_schema | api_owner | Freeze public contract before coding |
| `security_controls` | security | security_reviewer | Verify security baseline before release |
| `production_readiness` | release_readiness | release_manager | Final sign-off before deployment |

### Failure Policy

```
Node failure → retry (up to 3 times)
             → still failing → mark FAILED
                             → invalidate all transitive dependents → BLOCKED
                             → alert actor / trigger safe-stop if critical
```

Safe-stop conditions: `critical_security_risk`, `compliance_violation`, `missing_required_approval`, `rollback_failure`

---

## 3. Control Flow

### Create Short URL

```
Client
  │
  │  POST /api/v1/urls
  │  { "destinationUrl": "https://example.com", "expiresAt": "..." }
  ▼
UrlShortenerController
  │  extract destinationUrl, parse expiresAt
  │
  ▼
UrlShortenerService.create(destinationUrl, expiresAt)
  │  validateDestination()  ← reject non-HTTP(S), missing host, length > 2048
  │  generate 8-char code (SecureRandom, Base64)
  │  retry on collision (findByCode → exists → regenerate)
  │  persist ShortUrl entity
  │
  ▼
ShortUrlRepository.save()  →  H2 short_url table
  │
  ▼
Return 201 { code, shortUrl, destinationUrl, createdAt }
```

### Redirect + Click Recording

```
Client
  │
  │  GET /r/{code}
  ▼
UrlShortenerController
  │
  ▼
UrlShortenerService.redirectAndRecordClick(shortUrl)
  │  findByCode(code)          ← 404 if not found
  │  isActiveAt(Instant.now()) ← 404 if expired or DISABLED
  │  find or create hourly bucket (bucketStart = floor to hour)
  │  clickAggregate.increment()
  │  save
  │
  ▼
Return 302  Location: <destinationUrl>
```

### Analytics Query

```
Client
  │
  │  GET /api/v1/urls/{code}/analytics
  ▼
UrlShortenerController
  │
  ▼
UrlShortenerService.getAnalyticsCount(shortUrl)
  │  SELECT SUM(click_count) FROM click_aggregate WHERE short_url_id = ?
  │
  ▼
Return 200 { code, totalClicks, from, to }
```

### Orchestration Execution

```
OrchestratorMain (or test harness)
  │
  │  new WorkflowState(runId, version)
  │  new ApprovalManager(state) + setApprover(role, userId) × N
  │  new WorkflowExecutor(state, approvalManager)
  │  registerAgent(owner, agentInstance) × 4
  │  buildWorkflowDAG() → nodes with dependsOn
  │  initializeApprovals() → pre-approved gates
  │
  ▼
WorkflowExecutor.executeWorkflow()
  │
  │  while not done:
  │    findReadyNode()   ← PENDING or READY with all deps SUCCEEDED
  │    if node has gate → check ApprovalManager.isGateApproved()
  │      → not approved: transition to AWAITING_APPROVAL, skip
  │    node.setStatus(RUNNING)
  │    agents.get(node.owner).execute(node, state)
  │      → success: node SUCCEEDED, capture output
  │      → failure: incrementRetry
  │        → retryCount < 3: node back to READY
  │        → retryCount == 3: node FAILED, invalidate dependents
  │
  ▼
WorkflowState reflects terminal statuses + audit trail
```

---

## 4. Key Decisions (ADR Summary)

### ADR-001 — Governed Stateful Orchestration

**Decision**: Use a version-controlled DAG with a durable state ledger rather than ad-hoc scripting.

**Rationale**:
- Reproducibility: same DAG + same approvals → same execution order every time
- Auditability: every transition recorded with timestamp and actor
- Human control: gates prevent autonomous deployment or irreversible changes

**Trade-off**: More ceremony than a simple script; justified by governance requirements and the need to demonstrate safe agentic SDLC.

---

### ADR-002 — Versioned REST API

**Decision**: Expose APIs under `/api/v1`; treat `openapi.yaml` as the contract source of truth.

**Rationale**:
- Contract-first ensures frontend and backend stay aligned
- Version prefix allows non-breaking iteration
- Incompatible changes require explicit approval, preventing silent breakage

**Trade-off**: Must update `openapi.yaml` before implementing changes; adds a lightweight design step.

---

### ADR-003 — Relational Persistence

**Decision**: Relational schema (H2 for dev, PostgreSQL migration path) with explicit constraints and aggregate analytics table.

**Rationale**:
- `UNIQUE(code)` constraint prevents collision persistence
- `UNIQUE(short_url_id, bucket_start)` guarantees idempotent click aggregation
- Forward-only migration discipline ensures rollback safety

**Trade-off**: Event-level analytics deferred; aggregate-only is simpler and avoids PII risk.

---

### ADR-004 — Default-Deny Security Controls

**Decision**: HTTP(S)-only destinations, input-length limits, rate-limit defaults, no secrets in source.

**Rationale**:
- HTTP(S)-only closes SSRF vector (no `file://`, `gopher://`)
- Input limits prevent abuse and oversized URL storage
- Default-deny is safer to relax than to tighten retroactively

**Trade-off**: `mailto:`, `ftp://` etc. cannot be shortened; acceptable for a general-purpose HTTP URL shortener.

---

## 5. Component Responsibility Matrix

| Component | Owns | Does NOT own |
|-----------|------|-------------|
| `UrlShortenerController` | HTTP layer, request parsing, status codes | Business logic, validation rules |
| `UrlShortenerService` | Business logic, validation, collision retry, expiry | HTTP protocol, persistence details |
| `ShortUrlRepository` | Persistence queries (findByCode, save) | URL lifecycle rules |
| `ClickAggregateRepository` | Click bucket queries and sums | Click recording logic |
| `WorkflowExecutor` | DAG traversal, dependency resolution, retry | Agent-specific work, approval decisions |
| `ApprovalManager` | Gate state, role authorization, audit timestamps | Workflow execution order |
| `*Agent` classes | Domain-specific phase work, structured output | Workflow sequencing, gate enforcement |
| `WorkflowState` | In-memory canonical workflow state | Persistence to disk (JSON file is external) |

---

## 6. Non-Functional Constraints

| Concern | Constraint | Implementation |
|---------|-----------|---------------|
| **SSRF** | No server-side fetch of destination URL | Validation rejects non-HTTP(S) at creation |
| **Collision** | Code uniqueness required | Retry loop with `findByCode` check; DB `UNIQUE` as backstop |
| **Expiry** | Expired URLs return 404 | `isActiveAt(Instant.now())` checked on every redirect |
| **Click idempotency** | Hourly aggregation, no double-count | `UNIQUE(short_url_id, bucket_start)` + find-or-create pattern |
| **Gate bypass** | No node can self-approve | `ApprovalManager` enforces role → userId mapping set externally |
| **Retry bound** | No infinite retry loops | `MAX_RETRIES = 3` hard-coded; excess → FAILED + dependent invalidation |
| **Audit** | All transitions traceable | `WorkflowState.transitions[]` captures actor, timestamp, reason |

---

## 7. Technology Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| Backend framework | Spring Boot | 3.3.12 | Mature, production-grade, rich testing ecosystem |
| Language | Java | 21 (LTS) | Long-term support, modern records/sealed types |
| Persistence | Spring Data JPA + H2 | — | Zero-config for dev; PostgreSQL swap is config-only |
| Frontend | Angular | 19 | Standalone components, modern control flow, HttpClient |
| Build | Maven | 3.9 | Reproducible builds, exec plugin for orchestrator |
| Testing | JUnit 5 + Mockito | — | Standard Java test stack; Parameterized tests for coverage |
| Logging | SLF4J + Logback | — | Structured logging for audit trail and observability |
| API Spec | OpenAPI | 3.0 | Contract source of truth; enables validation and mocking |
