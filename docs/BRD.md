# Business Requirements Document (BRD)

**Project**: Agentic URL Shortener  
**Version**: 1.0.0  
**Date**: 2026-07-30  
**Status**: APPROVED  
**Document Owner**: Product Owner  

---

## 1. Executive Summary

This document defines the business requirements for the Agentic URL Shortener — a dual-purpose system delivering a production-oriented URL shortening service alongside a governed, stateful agentic SDLC orchestration layer. The business objective is to demonstrate that an AI-assisted software delivery lifecycle can produce a traceable, auditable, human-governed product while enforcing quality gates and generating defensible engineering evidence.

---

## 2. Business Context

### 2.1 Problem Statement

Software delivery pipelines commonly lack formal traceability between business intent, engineering decisions, implementation artifacts, and production readiness evidence. AI-assisted development amplifies this risk: agents may produce output without governance, approval, or auditability, creating unverifiable and potentially unsafe systems.

### 2.2 Business Opportunity

An agentic orchestration layer that enforces human approval gates, bounded retries, dependency ordering, and artifact lineage can:

- Reduce defects from unapproved architectural changes
- Provide an auditable decision trail for compliance and review
- Accelerate delivery by automating bounded, safe sub-tasks
- Demonstrate trustworthy AI-assisted engineering at assignment and enterprise scale

### 2.3 Stakeholders

| Role | Name / Alias | Interest |
|------|-------------|---------|
| Product Owner | alice@company.com | Requirements completeness, user value |
| Engineering Lead | bob@company.com | Architecture soundness, ADR approval |
| API Owner | charlie@company.com | Contract stability, backward compatibility |
| Security Reviewer | diana@company.com | Security baseline, privacy compliance |
| Release Manager | eve@company.com | Production readiness, deployment approval |
| End User | Anonymous browser user | Reliable URL creation and redirects |

---

## 3. Business Goals and Objectives

| ID | Goal | Metric | Target |
|----|------|--------|--------|
| BG-01 | Deliver a working URL shortener service | Endpoints operational, tests passing | 3 endpoints, 0 test failures |
| BG-02 | Demonstrate governed agentic SDLC | All approval gates enforced and recorded | 5 gates, audit trail present |
| BG-03 | Ensure traceability from requirement to release | Every decision linked to ADR or gate | 4 ADRs, decision lineage complete |
| BG-04 | Protect end users from unsafe redirects | SSRF and open-redirect prevented | HTTP(S)-only validation, tests passing |
| BG-05 | Preserve user privacy in analytics | No PII in analytics data | Aggregate-only clicks, no IP/UA stored |
| BG-06 | Enable future enhancement without regression | Brownfield migration plan exists | Migration plan in scenario-demonstrations.md |

---

## 4. Functional Requirements

### 4.1 URL Management (FR-01)

**Business Need**: Users must be able to convert long destination URLs into short, shareable codes.

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-01-a | Accept a destination URL and return a shortened code and URL | Must-Have | POST /api/v1/urls returns 201 with code and shortUrl |
| FR-01-b | Validate that the destination URL uses HTTP or HTTPS | Must-Have | Non-HTTP(S) URLs rejected with 400 |
| FR-01-c | Validate destination URL length does not exceed 2048 characters | Must-Have | Oversized URLs rejected with 400 |
| FR-01-d | Guarantee code uniqueness via collision-safe generation | Must-Have | Duplicate codes never persisted; retry on collision |
| FR-01-e | Accept an optional expiration timestamp for the short URL | Should-Have | expiresAt parameter accepted; stored correctly |

### 4.2 Redirect and Click Tracking (FR-02)

**Business Need**: Short codes must resolve to their destination URLs and track engagement.

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-02-a | Redirect a valid short code to its destination URL | Must-Have | GET /r/{code} returns 302 with Location header |
| FR-02-b | Return 404 for unknown or disabled codes | Must-Have | Non-existent codes return 404 |
| FR-02-c | Return 404 for expired URLs | Must-Have | URLs past expiresAt return 404 |
| FR-02-d | Record a click on every successful redirect | Must-Have | click_aggregate row created or incremented |
| FR-02-e | Aggregate clicks into hourly buckets | Should-Have | bucket_start = truncated to hour |

### 4.3 Analytics (FR-03)

**Business Need**: URL owners must be able to see aggregate click counts for their short URLs.

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-03-a | Return total click count for a given code | Must-Have | GET /api/v1/urls/{code}/analytics returns totalClicks |
| FR-03-b | Include time range (from/to) in the analytics response | Should-Have | Analytics response contains from and to fields |
| FR-03-c | Return 404 for analytics requests on unknown codes | Must-Have | Unknown code returns 404 |
| FR-03-d | Expose only aggregate counts, not event-level data | Must-Have | No IP, user-agent, or individual events in response |

### 4.4 Scenario Support (FR-04)

**Business Need**: The system must be demonstrably designed for greenfield, brownfield, and ambiguous requirement scenarios.

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-04-a | Document greenfield build workflow | Must-Have | scenario-demonstrations.md greenfield section complete |
| FR-04-b | Document brownfield change analysis and migration plan | Must-Have | scenario-demonstrations.md brownfield section complete |
| FR-04-c | Document ambiguous requirement resolution process | Must-Have | Ambiguity options enumerated, default justified, gate present |

### 4.5 Orchestration and Governance (FR-05)

**Business Need**: SDLC work must be coordinated through an explicit, human-governed workflow with state, retries, approvals, and traceability.

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-05-a | Define workflow as a versioned DAG with 18 nodes | Must-Have | execution-dag.yaml with dependency declarations |
| FR-05-b | Enforce node dependency ordering | Must-Have | WorkflowExecutor blocks nodes until dependencies SUCCEEDED |
| FR-05-c | Require human approval at 5 designated gates | Must-Have | ApprovalManager enforced; gated nodes pause until approved |
| FR-05-d | Retry failed nodes up to 3 times before marking FAILED | Must-Have | MAX_RETRIES = 3 in WorkflowExecutor |
| FR-05-e | Record all state transitions with timestamp and actor | Must-Have | WorkflowState.transitions[] populated on every change |
| FR-05-f | Provide at least 4 specialized agents | Must-Have | Requirements, Implementation, Testing, Documentation agents |
| FR-05-g | Agents must not self-approve protected gates | Must-Have | ApprovalManager.approveGate() requires external role binding |

---

## 5. Non-Functional Requirements

### 5.1 Security

| ID | Requirement |
|----|-------------|
| NFR-SEC-01 | Destination URLs validated as HTTP or HTTPS only; other schemes rejected |
| NFR-SEC-02 | No server-side fetch of destination URL to prevent SSRF |
| NFR-SEC-03 | Input length bounded (2048 chars for URL, reasonable limits on other fields) |
| NFR-SEC-04 | No credentials or secrets committed to source control |
| NFR-SEC-05 | Security gate required before release; security reviewer must approve |

### 5.2 Privacy

| ID | Requirement |
|----|-------------|
| NFR-PRI-01 | Analytics stores only aggregate click counts, not individual events |
| NFR-PRI-02 | No IP addresses, user agents, or referrer data stored |
| NFR-PRI-03 | Event-level analytics requires explicit privacy/retention approval before implementation |

### 5.3 Reliability

| ID | Requirement |
|----|-------------|
| NFR-REL-01 | Collision-resistant code generation with bounded retry |
| NFR-REL-02 | Database unique constraints enforce code and click-bucket uniqueness |
| NFR-REL-03 | Health and readiness endpoints exposed for monitoring |
| NFR-REL-04 | Orchestration workflow supports safe-stop and rollback |

### 5.4 Maintainability

| ID | Requirement |
|----|-------------|
| NFR-MNT-01 | All architectural decisions recorded in ADRs |
| NFR-MNT-02 | OpenAPI contract is authoritative; implementation must not diverge |
| NFR-MNT-03 | Schema changes require migration plan and approval |
| NFR-MNT-04 | Agents must be independently testable |

### 5.5 Testability

| ID | Requirement |
|----|-------------|
| NFR-TST-01 | Minimum 80% line coverage on business logic |
| NFR-TST-02 | Minimum 70% branch coverage on business logic |
| NFR-TST-03 | All API endpoints covered by integration tests |
| NFR-TST-04 | Orchestration engine covered by dedicated test class |

---

## 6. Business Constraints

| ID | Constraint | Impact |
|----|-----------|--------|
| BC-01 | Java 21 + Spring Boot 3.3 for backend | Technology stack fixed |
| BC-02 | Angular 19 for frontend | Frontend framework fixed |
| BC-03 | No event-level analytics in MVP | Privacy risk management |
| BC-04 | No authentication in MVP | Scope management; brownfield enhancement path documented |
| BC-05 | H2 in-memory database for development | PostgreSQL migration documented for production |
| BC-06 | No autonomous deployment by agents | Human approval required at all release gates |

---

## 7. Out of Scope (MVP)

- User authentication and account management
- Custom short codes (user-specified)
- QR code generation
- Event-level analytics (IP, user-agent, referrer)
- Rate limiting with SLO enforcement (default config only)
- Multi-tenant isolation
- Custom domain support

All items above are candidates for brownfield enhancements and are documented in `docs/requirements/scenario-demonstrations.md`.

---

## 8. Assumptions

| ID | Assumption |
|----|-----------|
| A-01 | All approval gates will be exercised by named human roles before production deployment |
| A-02 | H2 in-memory database is acceptable for demonstration; production requires PostgreSQL migration |
| A-03 | Base URL `http://localhost:8080` is acceptable for development; configurable via AppProperties |
| A-04 | Aggregate-only analytics satisfies the analytics requirement for MVP |
| A-05 | A single Spring Boot process is sufficient for demonstration scale |

---

## 9. Approval Sign-off

| Role | Approver | Approval ID | Date |
|------|---------|------------|------|
| Product Owner | alice@company.com | APR-001 | 2026-07-30 |
| Engineering Lead | bob@company.com | APR-002 | 2026-07-30 |
| API Owner | charlie@company.com | APR-003 | 2026-07-30 |
| Security Reviewer | diana@company.com | APR-004 | 2026-07-30 |
| Release Manager | eve@company.com | APR-005 | 2026-07-30 |
