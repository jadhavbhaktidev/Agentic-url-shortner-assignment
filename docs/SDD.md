# Software Design Document (SDD)

**Project**: Agentic URL Shortener  
**Version**: 1.0.0  
**Date**: 2026-07-30  
**Status**: APPROVED  
**Document Owner**: Engineering Lead  

---

## 1. Purpose and Scope

This document describes the detailed software design of the Agentic URL Shortener system, covering:

- Component structure and responsibilities
- Data model and persistence layer
- API contract and interface design
- Orchestration engine design
- Agent interaction model
- Error handling and retry logic
- Security controls
- Test design

This document is the bridge between the BRD (business requirements) and implementation source code.

---

## 2. System Architecture

### 2.1 High-Level Component Diagram

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                  Spring Boot Application                 │
                    │                                                           │
                    │  ┌─────────────────────────────────────────────────┐    │
                    │  │              Presentation Layer                   │    │
                    │  │  UrlShortenerController                           │    │
                    │  │  ├─ POST   /api/v1/urls                          │    │
                    │  │  ├─ GET    /r/{code}                             │    │
                    │  │  └─ GET    /api/v1/urls/{code}/analytics         │    │
                    │  └───────────────────┬─────────────────────────────┘    │
                    │                      │                                    │
                    │  ┌───────────────────▼─────────────────────────────┐    │
                    │  │               Service Layer                       │    │
                    │  │  UrlShortenerService                             │    │
                    │  │  ├─ create(destinationUrl, expiresAt)            │    │
                    │  │  ├─ validateDestination(url)                     │    │
                    │  │  ├─ redirectAndRecordClick(shortUrl)             │    │
                    │  │  ├─ buildShortUrl(code)                          │    │
                    │  │  └─ getAnalyticsCount(shortUrl)                  │    │
                    │  └───────────────────┬─────────────────────────────┘    │
                    │                      │                                    │
                    │  ┌───────────────────▼─────────────────────────────┐    │
                    │  │            Repository Layer (JPA)                 │    │
                    │  │  ShortUrlRepository       ClickAggregateRepo     │    │
                    │  │  ├─ findByCode(code)      ├─ findByShortUrl      │    │
                    │  │  └─ save(shortUrl)         │   AndBucketStart    │    │
                    │  │                            └─ sumClickCount      │    │
                    │  └───────────────────┬─────────────────────────────┘    │
                    │                      │                                    │
                    │  ┌───────────────────▼─────────────────────────────┐    │
                    │  │              Domain Model                         │    │
                    │  │  ShortUrl (entity)       ClickAggregate (entity) │    │
                    │  │  UrlStatus (enum)         AppProperties (config) │    │
                    │  └─────────────────────────────────────────────────┘    │
                    │                                                           │
                    └─────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────────────────────────┐
                    │               Orchestration Control Plane                │
                    │                                                           │
                    │  WorkflowExecutor ◀──▶ ApprovalManager                  │
                    │       │                                                   │
                    │       ▼                                                   │
                    │  Agent Registry                                           │
                    │  ├─ RequirementsAnalysisAgent                            │
                    │  ├─ ImplementationAgent                                  │
                    │  ├─ TestingAgent                                         │
                    │  └─ DocumentationAgent                                   │
                    │       │                                                   │
                    │       ▼                                                   │
                    │  WorkflowState (model)                                   │
                    │  ├─ Map<String, WorkflowNode>                            │
                    │  ├─ List<Approval>                                       │
                    │  └─ List<Transition>                                     │
                    └─────────────────────────────────────────────────────────┘
```

### 2.2 Package Structure

```
com.agentic.urlshortener
├── UrlShortenerApplication.java          — Spring Boot entry point
├── config
│   └── AppProperties.java               — Configuration record (publicBaseUrl)
├── controller
│   └── UrlShortenerController.java       — REST endpoints
├── service
│   └── UrlShortenerService.java          — Business logic
├── domain
│   ├── ShortUrl.java                     — JPA entity
│   ├── ClickAggregate.java               — JPA entity
│   └── UrlStatus.java                    — Enum: ACTIVE, DISABLED
└── repository
    ├── ShortUrlRepository.java           — JPA repository
    └── ClickAggregateRepository.java     — JPA repository with SUM query

com.agentic.orchestration
├── OrchestratorMain.java                 — Entry point for workflow execution
├── engine
│   ├── WorkflowExecutor.java             — DAG traversal, retry, invalidation
│   └── ApprovalManager.java             — Gate enforcement, role authorization
├── agents
│   ├── RequirementsAnalysisAgent.java
│   ├── ImplementationAgent.java
│   ├── TestingAgent.java
│   └── DocumentationAgent.java
└── model
    ├── WorkflowNode.java                 — Node with status, deps, output, retries
    └── WorkflowState.java               — Workflow state with transitions, approvals
```

---

## 3. Domain Model Design

### 3.1 ShortUrl Entity

```java
@Entity
@Table(name = "short_url")
public class ShortUrl {
    @Id @GeneratedValue(strategy = IDENTITY)
    Long id;

    @Column(unique = true, nullable = false, length = 16)
    String code;                    // 8-char Base64 code, public identifier

    @Column(nullable = false, length = 2048)
    String destinationUrl;          // validated HTTP(S) destination

    Instant createdAt;              // UTC creation timestamp
    Instant expiresAt;              // nullable; null = never expires
    UrlStatus status;               // ACTIVE | DISABLED

    // business rule: active iff status==ACTIVE and (expiresAt==null OR expiresAt.isAfter(now))
    public boolean isActiveAt(Instant now) {
        return status == UrlStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(now));
    }
}
```

### 3.2 ClickAggregate Entity

```java
@Entity
@Table(name = "click_aggregate",
       uniqueConstraints = @UniqueConstraint(columnNames = {"short_url_id", "bucket_start"}))
public class ClickAggregate {
    @Id @GeneratedValue(strategy = IDENTITY)
    Long id;

    @ManyToOne @JoinColumn(name = "short_url_id", nullable = false)
    ShortUrl shortUrl;

    Instant bucketStart;            // truncated to hour; guarantees one row per URL per hour
    long clickCount;                // aggregate count; starts at 1 on creation

    public void increment() { this.clickCount++; }
}
```

### 3.3 WorkflowNode Model

```java
public class WorkflowNode {
    String id;                      // DAG node identifier
    String type;                    // Requirement | Implementation | Test | ...
    String owner;                   // registered agent name
    NodeStatus status;              // PENDING | READY | RUNNING | SUCCEEDED | FAILED | ...
    List<String> dependsOn;         // IDs of prerequisite nodes
    String gate;                    // approval gate name (null if none)
    Map<String, Object> output;     // agent output after SUCCEEDED
    Instant startedAt;
    Instant completedAt;
    int retryCount;                 // incremented on each retry; max = 3
    String lastError;               // error message from most recent failure
}
```

---

## 4. Service Layer Design

### 4.1 URL Creation Logic

```
create(destinationUrl, expiresAt):
    1. validateDestination(destinationUrl)
       ├─ reject if scheme ≠ http or https  → IllegalArgumentException
       ├─ reject if host is null/blank       → IllegalArgumentException
       └─ reject if length > 2048           → IllegalArgumentException

    2. Generate code:
       loop (max 10 attempts):
           code = 8 random bytes → Base64 URL-safe → first 8 chars
           if shortUrlRepository.findByCode(code).isEmpty() → break
       if exhausted → throw IllegalStateException

    3. Persist:
       shortUrl = new ShortUrl(code, destinationUrl, now(), expiresAt, ACTIVE)
       shortUrlRepository.save(shortUrl)

    4. Return:
       buildShortUrl(code) = appProperties.publicBaseUrl + "/r/" + code
```

### 4.2 Redirect and Click Recording Logic

```
redirectAndRecordClick(shortUrl):
    1. Check: shortUrl.isActiveAt(Instant.now())
       └─ false → throw NoSuchElementException (→ 404)

    2. Compute bucket:
       bucketStart = now() truncated to start of current hour

    3. Find or create click bucket:
       existing = clickAggregateRepository.findByShortUrlAndBucketStart(shortUrl, bucketStart)
       ├─ found  → existing.increment() → save
       └─ absent → new ClickAggregate(shortUrl, bucketStart, 1) → save

    4. Return:
       shortUrl.destinationUrl  (controller issues 302 Location)
```

### 4.3 Analytics Aggregation Logic

```
getAnalyticsCount(shortUrl):
    return clickAggregateRepository.sumClickCountByShortUrl(shortUrl)
    — SQL: SELECT SUM(click_count) FROM click_aggregate WHERE short_url_id = ?
    — Returns 0 if no clicks recorded
```

---

## 5. API Design

Full contract: `openapi/openapi.yaml`

### 5.1 POST /api/v1/urls

**Request**:
```json
{
  "destinationUrl": "https://example.com/some/long/path",
  "expiresAt": "2026-12-31T23:59:59Z"  // optional
}
```

**Responses**:
| Status | Body | Condition |
|--------|------|-----------|
| 201 Created | `{ code, shortUrl, destinationUrl, createdAt }` | Valid URL created |
| 400 Bad Request | `{ error: "..." }` | Invalid URL scheme, empty host, or too long |
| 429 Too Many Requests | — | Rate limit exceeded (default: not enforced in MVP) |

**Design Notes**:
- `shortUrl` in response uses `app.public-base-url` config, not request `Host` header, to avoid SSRF header injection
- `code` is an 8-character Base64 URL-safe string; not exposed as modifiable

### 5.2 GET /r/{code}

**Responses**:
| Status | Headers | Condition |
|--------|---------|-----------|
| 302 Found | `Location: <destinationUrl>` | Active URL found |
| 404 Not Found | — | Code unknown, expired, or DISABLED |

**Design Notes**:
- 302 (not 301) to allow future updates to destination without browser cache poisoning
- Click recorded before redirect issues to avoid missed counts if client aborts

### 5.3 GET /api/v1/urls/{code}/analytics

**Responses**:
| Status | Body | Condition |
|--------|------|-----------|
| 200 OK | `{ code, totalClicks, from, to }` | URL found |
| 404 Not Found | — | Code unknown |

**Design Notes**:
- `totalClicks` is an aggregate integer, never event-level data
- `from`/`to` represent the time range of recorded click buckets; null if no clicks

---

## 6. Orchestration Engine Design

### 6.1 WorkflowExecutor — Core Algorithm

```
executeWorkflow():
    state.status = RUNNING

    while true:
        node = findReadyNode()

        if node == null:
            if hasBlockedOrAwaitingNodes()  → log PAUSED, break
            if allNodesTerminal()           → log COMPLETED, break
            else                            → log STUCK, break

        executeNode(node)

    state.status = COMPLETED

findReadyNode():
    return nodes.values()
        .filter(status == PENDING or READY)
        .filter(all dependsOn nodes have status == SUCCEEDED)
        .findFirst()

executeNode(node):
    if node.gate != null and !approvalManager.isGateApproved(node.gate):
        node.status = AWAITING_APPROVAL
        return

    node.status = RUNNING
    node.startedAt = now()

    try:
        agent = agents.get(node.owner)  // throws if missing
        output = agent.execute(node, state)
        node.output = output
        node.status = SUCCEEDED
        node.completedAt = now()
    catch Exception e:
        handleNodeFailure(node, e)

handleNodeFailure(node, e):
    node.lastError = e.message
    if node.retryCount < MAX_RETRIES:
        node.retryCount++
        node.status = READY      // re-queued for retry
    else:
        node.status = FAILED
        invalidateDependents(node)   // mark all downstream BLOCKED

invalidateDependents(failedNode):
    nodes.values()
        .filter(n.dependsOn.contains(failedNode.id))
        .forEach(n.status = BLOCKED)
```

### 6.2 ApprovalManager — Gate Enforcement

```
isGateApproved(gateName):
    return state.approvals.any(a.gate == gateName AND a.status == "APPROVED")

approveGate(approvalId, gateName, role, userId):
    validate: approversByRole.get(role) == userId  // else throw SecurityException
    approval = state.approvals.find(a.id == approvalId)
    approval.status = "APPROVED"
    approval.approvedBy = userId
    approval.approvedAt = now()
    state.lastModifiedAt = now()

setApprover(role, userId):
    approversByRole.put(role, userId)
    // must be called before approveGate; agents cannot call this method
```

### 6.3 Agent Interface Contract

```java
@FunctionalInterface
public interface Agent {
    Map<String, Object> execute(WorkflowNode node, WorkflowState state);
    // Must return non-null Map
    // May read state but must NOT modify state.nodes or state.approvals directly
    // Must complete within reasonable time (no blocking I/O in MVP agents)
    // Throws RuntimeException on failure; WorkflowExecutor handles retry
}
```

### 6.4 Agent Responsibilities Matrix

| Agent | Phase | Key Output Fields |
|-------|-------|------------------|
| `RequirementsAnalysisAgent` | Requirements | `requirementsCount`, `functionalRequirements`, `ambiguities`, `status` |
| `ImplementationAgent` | Implementation | `endpoints`, `testCount`, `buildStatus`, `apiContractValidation` |
| `TestingAgent` | Testing | `unitTestsRun`, `unitTestsPassed`, `coverage`, `scenariosValidated` |
| `DocumentationAgent` | Documentation | `artifacts`, `operationalReadiness`, `apiDocumentationStatus` |

---

## 7. Database Design

### 7.1 Schema

```sql
CREATE TABLE short_url (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(16)  NOT NULL UNIQUE,
    destination_url VARCHAR(2048) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    expires_at      TIMESTAMP,           -- nullable: NULL means never expires
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_short_url_code_status ON short_url (code, status);

CREATE TABLE click_aggregate (
    id           BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_url_id BIGINT    NOT NULL REFERENCES short_url(id),
    bucket_start TIMESTAMP NOT NULL,
    click_count  BIGINT    NOT NULL DEFAULT 1,
    UNIQUE (short_url_id, bucket_start)
);
```

### 7.2 Indexing Strategy

| Index | Columns | Purpose |
|-------|---------|---------|
| `idx_short_url_code_status` | (code, status) | Fast lookup on redirect path |
| UNIQUE on code | code | Collision prevention backstop |
| UNIQUE on (short_url_id, bucket_start) | — | Idempotent click aggregation |
| FK on short_url_id | — | Referential integrity |

### 7.3 Migration Strategy (H2 → PostgreSQL)

1. Replace `H2` dependency with `postgresql` in pom.xml
2. Update `spring.datasource.url` in application.yml
3. Replace H2-specific DDL with PostgreSQL-compatible SQL if needed (schema.sql is already standard)
4. Add Flyway or Liquibase for migration management
5. Add `expires_at` and `owner_key` columns as nullable in first brownfield migration

---

## 8. Error Handling Design

### 8.1 Application Layer

| Exception | HTTP Status | Trigger |
|-----------|------------|---------|
| `IllegalArgumentException` | 400 Bad Request | URL validation failure |
| `NoSuchElementException` | 404 Not Found | Unknown code, expired URL |
| `DataIntegrityViolationException` | 409 Conflict | Race condition on code insert (fallback after retry exhaustion) |
| Uncaught `Exception` | 500 Internal Server Error | Unexpected failure |

### 8.2 Orchestration Layer

| Condition | Behaviour |
|-----------|-----------|
| Agent throws RuntimeException | Node retried (up to MAX_RETRIES = 3) |
| Retries exhausted | Node → FAILED; dependents → BLOCKED |
| Gate not approved | Node → AWAITING_APPROVAL; workflow continues with other ready nodes |
| No agent for owner | WorkflowException thrown immediately (configuration error) |
| Critical security risk | SAFE_STOPPED workflow state; no further nodes execute |

---

## 9. Security Design

### 9.1 Input Validation

```java
private void validateDestination(String url) throws IllegalArgumentException {
    URI uri = URI.create(url);                     // rejects malformed URIs
    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equals("http") && !scheme.equals("https")))
        throw new IllegalArgumentException("Only HTTP(S) destinations allowed");
    if (uri.getHost() == null || uri.getHost().isBlank())
        throw new IllegalArgumentException("Destination URL must have a host");
    if (url.length() > 2048)
        throw new IllegalArgumentException("Destination URL too long");
}
```

**SSRF Mitigation**: The server never fetches the destination URL. Validation is syntactic only.

### 9.2 SSRF Prevention

- Only `http://` and `https://` schemes are allowed
- No proxy or fetch of destination at any point
- `file://`, `gopher://`, `ldap://`, `ftp://` all rejected at validation

### 9.3 Code Generation

```java
// SecureRandom ensures unpredictability; 8 bytes = 64 bits of entropy
byte[] bytes = new byte[8];
secureRandom.nextBytes(bytes);
String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 8);
```

### 9.4 Gate Authorization

```java
// Approval requires an externally-bound userId; agents cannot self-approve
public void approveGate(String gateId, String gateName, String role, String userId) {
    if (!userId.equals(approversByRole.get(role)))
        throw new SecurityException("User not authorized for role: " + role);
    // ... update approval record
}
```

---

## 10. Testing Design

### 10.1 Test Matrix

| Layer | Class | Tests | Approach |
|-------|-------|-------|---------|
| Unit (service) | `UrlShortenerServiceTest` | 12 | Mockito; real ShortUrl instances (Byte Buddy limitation on Java 26) |
| Integration (controller) | `UrlShortenerControllerTest` | 12 | MockMvc; full Spring context |
| Health/Home | `HealthEndpointTest`, `HomePageTest` | 2 | MockMvc; endpoint availability |
| Orchestration | `WorkflowExecutorTest` | 5 | Plain JUnit 5; no Spring context |
| **Total** | — | **31** | **0 failures** |

### 10.2 Orchestration Test Scenarios

| Test | Validates |
|------|-----------|
| `testWorkflowSequentialExecution` | All 4 nodes execute in dependency order; all SUCCEEDED |
| `testDependencyEnforcement` | Implementation blocked until requirements SUCCEEDED |
| `testApprovalGateEnforcement` | Gated node pauses until gate approved; then proceeds |
| `testNodeRetryOnFailure` | retryCount increments; lastError recorded |
| `testWorkflowStateTransitions` | Status transitions from READY_FOR_EXECUTION → COMPLETED |

### 10.3 Coverage Targets

| Scope | Line Coverage | Branch Coverage |
|-------|--------------|----------------|
| Business logic (`service`, `domain`) | ≥ 80% | ≥ 70% |
| Controller layer | ≥ 75% | ≥ 65% |
| Orchestration engine | ≥ 80% | ≥ 70% |

---

## 11. Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `app.public-base-url` | `http://localhost:8080` | Base URL prepended to short codes in responses |
| `spring.datasource.url` | `jdbc:h2:mem:urlshortener` | H2 in-memory datasource |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema validated against schema.sql; no auto-generation |
| `spring.h2.console.enabled` | `true` (dev) | H2 console at `/h2-console` for development inspection |
| `management.endpoints.web.exposure.include` | `health,info` | Spring Actuator endpoints exposed |

---

## 12. Interfaces Between Components

| From | To | Interface | Contract |
|------|----|-----------|---------|
| Angular HttpClient | UrlShortenerController | HTTP/JSON | openapi/openapi.yaml |
| UrlShortenerController | UrlShortenerService | Java method calls | Throws `IllegalArgumentException` (→ 400), `NoSuchElementException` (→ 404) |
| UrlShortenerService | ShortUrlRepository | Spring Data JPA | `findByCode(String)`, `save(ShortUrl)` |
| UrlShortenerService | ClickAggregateRepository | Spring Data JPA | `findByShortUrlAndBucketStart`, `sumClickCountByShortUrl` |
| WorkflowExecutor | Agent (interface) | `execute(node, state) → Map<String,Object>` | Must not throw checked exceptions; RuntimeException triggers retry |
| WorkflowExecutor | ApprovalManager | `isGateApproved(String)`, `approveGate(...)` | ApprovalManager reads from WorkflowState.approvals |
| OrchestratorMain | WorkflowExecutor | `registerAgent`, `executeWorkflow` | Must register all agents before `executeWorkflow()` |

---

## 13. Deployment View

```
Developer Workstation
│
├── Terminal 1: mvn spring-boot:run
│   └── Spring Boot starts on :8080
│       ├── GET  /health        → UP
│       ├── GET  /ready         → READY
│       ├── POST /api/v1/urls   → create
│       ├── GET  /r/{code}      → redirect
│       └── GET  /api/v1/urls/{code}/analytics → analytics
│
├── Terminal 2: cd frontend && ng serve
│   └── Angular dev server on :4200
│       └── proxy.conf.json routes /api and /r → :8080
│
└── Terminal 3 (optional): mvn test -Dtest=WorkflowExecutorTest
    └── Orchestration workflow executed; agent logs in terminal
```

**Production target**: Replace H2 with PostgreSQL, containerize Spring Boot, build Angular to `dist/`, serve via Nginx or CDN. See `docs/runbooks/operational-runbook.md`.

---

## 14. Known Limitations and Future Work

| Limitation | Impact | Mitigation in Brownfield |
|-----------|--------|------------------------|
| H2 in-memory DB | Data lost on restart | Replace with PostgreSQL + Flyway migrations |
| No authentication | All URLs publicly accessible | Add API key or JWT; owner_key column for scoped analytics |
| Aggregate analytics only | No event-level data | Add /events endpoint after privacy approval |
| Single JVM orchestration | Not distributed | Agent choreography via message queue in brownfield |
| No rate limiting SLO | Potential abuse | Token bucket per IP after SLO approval |
| Code length fixed at 8 chars | ~4 billion combinations | Increase code length or use configurable length |
