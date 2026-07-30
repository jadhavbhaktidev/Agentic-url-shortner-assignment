# Product Design Document (PDD)

**Project**: Agentic URL Shortener  
**Version**: 1.0.0  
**Date**: 2026-07-30  
**Status**: APPROVED  
**Document Owner**: Product Owner + Engineering Lead  

---

## 1. Product Vision

The Agentic URL Shortener is a dual-purpose product:

1. **URL Shortener Service**: A clean, reliable service that creates short URLs, redirects users, and reports aggregate click analytics.
2. **Agentic SDLC System**: A governed orchestration layer that demonstrates how AI agents can safely deliver software under human oversight, with full traceability from requirements to release.

The product is designed to be a reference implementation showing that agentic software delivery is tractable, auditable, and safe when approval gates and dependency ordering are enforced.

---

## 2. User Personas

### Persona 1 — Link Sharer
**Who**: A person with a long URL who wants a short, shareable link.  
**Goal**: Create a short URL quickly and share it.  
**Pain point**: Long URLs are unreadable in messages, emails, and social media.  
**Interaction**:
1. Opens the Angular frontend at `http://localhost:4200`
2. Pastes a long URL into the input field
3. Clicks "Create Short URL"
4. Copies the generated short URL and shares it

### Persona 2 — Link Owner
**Who**: A person who created a short URL and wants to know its performance.  
**Goal**: See how many times the short link has been clicked.  
**Pain point**: No visibility into link engagement without analytics.  
**Interaction**:
1. Opens the Angular frontend
2. Enters the short code into the analytics input field
3. Clicks "Load Analytics"
4. Reads total clicks and time range

### Persona 3 — Engineering Reviewer
**Who**: An engineering lead reviewing the delivery lifecycle artifacts.  
**Goal**: Verify that the SDLC followed proper governance.  
**Pain point**: Ad-hoc AI-generated code lacks traceability and justification.  
**Interaction**:
1. Reviews `execution/workflow/execution-dag.yaml` for process structure
2. Checks `execution/state/wf-20260730-001.json` for approval records
3. Reviews ADRs (`docs/architecture/adr/`) for decision rationale
4. Runs `mvn verify` to validate test evidence

---

## 3. User Journeys

### Journey 1: Create and Share a Short URL

```
User opens Angular UI (localhost:4200)
    │
    ├── Sees hero section: "Agentic URL Shortener"
    ├── Enters destination URL in the input field
    └── Clicks "Create Short URL"
            │
            ├── [Success] Result card displays:
            │     • Short code (e.g., a1b2c3d4)
            │     • Full short URL (http://localhost:8080/r/a1b2c3d4)
            │     • Original destination URL
            │     • Created timestamp
            │
            └── [Failure] Error message displayed:
                  • "Invalid URL" for non-HTTP(S) destinations
                  • "URL too long" for > 2048 characters
                  • Server error message for unexpected failures
```

### Journey 2: Load Analytics

```
User opens Angular UI
    │
    ├── Enters short code in analytics input
    └── Clicks "Load Analytics"
            │
            ├── [Success] Analytics card displays:
            │     • Short code
            │     • Total click count
            │     • From / To time range
            │
            └── [Failure] Error message displayed:
                  • "Code not found" for unknown codes
```

### Journey 3: Follow a Short Link

```
Someone receives short URL (e.g., http://localhost:8080/r/a1b2c3d4)
    │
    └── Browser GETs /r/a1b2c3d4
            │
            ├── [Active URL] Browser redirects (302) to destination
            │     • Click is recorded in hourly bucket
            │
            ├── [Expired URL] 404 response
            │
            └── [Unknown code] 404 response
```

---

## 4. Product Features

### Feature Set 1: URL Creation

| Feature | Description | Status |
|---------|-------------|--------|
| Create short URL | Accept destination URL, return code + shortUrl | ✅ Implemented |
| URL validation | Reject non-HTTP(S), empty host, > 2048 chars | ✅ Implemented |
| Expiration support | Optional expiresAt parameter for time-limited links | ✅ Implemented |
| Collision safety | Retry code generation on conflict, DB unique constraint backstop | ✅ Implemented |

### Feature Set 2: Redirect

| Feature | Description | Status |
|---------|-------------|--------|
| HTTP redirect | 302 Found with Location header to destination | ✅ Implemented |
| Expiry enforcement | Expired URLs return 404 | ✅ Implemented |
| Click recording | Increment hourly click bucket on every redirect | ✅ Implemented |
| Unknown code handling | 404 for unrecognised codes | ✅ Implemented |

### Feature Set 3: Analytics

| Feature | Description | Status |
|---------|-------------|--------|
| Aggregate clicks | SUM of click_count across all buckets for a code | ✅ Implemented |
| Time range | From/to timestamps in response | ✅ Implemented |
| Unknown code | 404 for analytics on unknown codes | ✅ Implemented |
| Owner-scoped access | ownerKey-based authorization | 🔶 Brownfield (deferred) |

### Feature Set 4: Orchestration

| Feature | Description | Status |
|---------|-------------|--------|
| 18-node workflow DAG | Dependency-ordered SDLC workflow | ✅ Implemented |
| Approval gates | 5 human-approval checkpoints | ✅ Implemented |
| Agent specialization | 4 distinct agents for SDLC phases | ✅ Implemented |
| Retry mechanism | Max 3 retries per node, dependent invalidation | ✅ Implemented |
| Audit trail | State transitions with timestamps and actors | ✅ Implemented |

---

## 5. UI Design

### 5.1 Layout Structure

```
┌─────────────────────────────────────────────────────────────┐
│                   Agentic URL Shortener                      │
│         Governed, traceable, agent-assisted delivery         │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Create Short URL                        │    │
│  │                                                      │    │
│  │  [ Enter destination URL ........................ ]  │    │
│  │                                                      │    │
│  │                    [ Create Short URL ]              │    │
│  │                                                      │    │
│  │  ┌──────────────────────────────────────────────┐   │    │
│  │  │ ✓ Result                                      │   │    │
│  │  │   Code:         a1b2c3d4                       │   │    │
│  │  │   Short URL:    http://localhost:8080/r/...   │   │    │
│  │  │   Destination:  https://example.com           │   │    │
│  │  │   Created:      2026-07-30T10:48:53Z          │   │    │
│  │  └──────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              URL Analytics                           │    │
│  │                                                      │    │
│  │  Code: [ a1b2c3d4 ]  [ Load Analytics ]             │    │
│  │                                                      │    │
│  │  ┌──────────────────────────────────────────────┐   │    │
│  │  │ Analytics for a1b2c3d4                        │   │    │
│  │  │   Total Clicks: 12                            │   │    │
│  │  │   From:  2026-07-30T09:00:00Z                 │   │    │
│  │  │   To:    2026-07-30T10:00:00Z                 │   │    │
│  │  └──────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  ✗ Error message (shown only on failure)             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Visual Design Principles

- **Background**: Linear gradient `#163c85 → #4f7cf3` to convey trustworthiness and technical depth
- **Cards**: White with border-radius 18px and box-shadow for elevation and clarity
- **Buttons**: Accent colour with hover state; disabled during loading to prevent double-submission
- **Error state**: Red border/text for inline error display without modal interruption
- **Loading state**: Button text changes to "Loading..." during async API calls

### 5.3 Responsive Behaviour

- Single-column card layout on all viewports
- Maximum content width constrained to prevent over-stretching on wide screens
- Form inputs are full-width within their card

---

## 6. API Design Summary

Full specification: `openapi/openapi.yaml`

| Endpoint | Method | Request | Response | Purpose |
|----------|--------|---------|----------|---------|
| `/api/v1/urls` | POST | `{ destinationUrl, expiresAt? }` | `201 { code, shortUrl, destinationUrl, createdAt }` | Create a short URL |
| `/r/{code}` | GET | — | `302 Location: <destination>` or `404` | Redirect and record click |
| `/api/v1/urls/{code}/analytics` | GET | — | `200 { code, totalClicks, from, to }` or `404` | Retrieve aggregate analytics |

### Design Decisions
- **POST for creation** (not GET) — creation is a state-changing operation; idempotency not guaranteed
- **302 vs 301** — 302 (temporary) allows destination URL to be updated in future brownfield enhancement; 301 would be cached by browsers indefinitely
- **Aggregate analytics only** — no event-level data to avoid PII; reversible decision (can add later via new endpoint)

---

## 7. Data Model

```
short_url
├── id             BIGINT IDENTITY PRIMARY KEY
├── code           VARCHAR(16) UNIQUE NOT NULL      — public identifier
├── destination_url VARCHAR(2048) NOT NULL           — validated HTTP(S) destination
├── created_at     TIMESTAMP NOT NULL               — creation time
├── expires_at     TIMESTAMP NULL                   — optional expiry (null = never expires)
└── status         VARCHAR(16) NOT NULL             — ACTIVE | DISABLED

click_aggregate
├── id             BIGINT IDENTITY PRIMARY KEY
├── short_url_id   BIGINT NOT NULL → short_url.id  — FK with cascade delete
├── bucket_start   TIMESTAMP NOT NULL               — floor to hour (UTC)
└── click_count    BIGINT NOT NULL DEFAULT 1        — aggregate count for this bucket
UNIQUE(short_url_id, bucket_start)
```

### Key Design Choices
- `code` is separate from `id`: ID is internal; code is the public-facing identifier and can be changed without breaking foreign keys
- Hourly bucketing in `click_aggregate` balances granularity vs. storage; daily or real-time possible in brownfield
- `expires_at` is nullable: NULL means the URL never expires, preserving backward compatibility

---

## 8. Deferred Features (Brownfield Backlog)

| Feature | Business Justification | Migration Complexity | Priority |
|---------|----------------------|---------------------|---------|
| Owner-scoped analytics | Analytics privacy and access control | Low — add `owner_key` column, nullable | High |
| Authentication / API keys | Multi-user support, abuse prevention | Medium — new user table, token validation | High |
| Custom short codes | User experience, brand consistency | Low — allow user-supplied code with conflict check | Medium |
| Event-level analytics | Detailed engagement data | High — privacy approval, schema change, retention policy | Low |
| Rate limiting with SLO | Abuse prevention, operational stability | Medium — token bucket per IP or API key | Medium |
| Custom domain support | White-label use cases | High — TLS, DNS, multi-tenancy | Low |

---

## 9. Success Metrics

| Metric | Target | How Measured |
|--------|--------|-------------|
| URL creation success rate | ≥ 99% for valid inputs | Controller integration tests |
| Redirect success rate | ≥ 99% for active URLs | Redirect integration tests |
| Test pass rate | 100% | `mvn verify` output |
| Line coverage on business logic | ≥ 80% | JaCoCo report |
| Branch coverage on business logic | ≥ 70% | JaCoCo report |
| Approval gates enforced | 5/5 gates with recorded approvals | WorkflowState approvals[] |
| Audit trail completeness | All node transitions recorded | WorkflowState transitions[] |
