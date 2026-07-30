# Agentic URL Shortener — Scenario Demonstrations

## Greenfield Scenario: Building from Scratch

### Objective
Deliver a complete URL shortener system from approved requirements through production readiness, using the agentic orchestration model.

### Workflow Execution Path
```
requirements (READY)
  ↓ [normalize & validate]
  ├─→ ambiguity (AWAITING_APPROVAL)
  ├─→ architecture (AWAITING_APPROVAL)
  │    ↓ [ADRs approved]
  │    └─→ api_schema (AWAITING_APPROVAL)
  │         ↓ [API approved]
  │         └─→ greenfield (READY)
  │              ├─→ backend implementation
  │              └─→ frontend implementation
  │                   ↓
  │                   └─→ integration (READY)
  └─→ test_prep (READY)
       └─→ testing (READY)
            ├─→ unit tests (26 pass)
            ├─→ integration tests (12 pass)
            ├─→ contract validation (PASSED)
            └─→ security (AWAITING_APPROVAL)
                 ↓ [security approved]
                 └─→ release_readiness (READY)
                      ├─→ all gates clear
                      └─→ final_summary (READY)
```

### Key Orchestration Decisions
1. **Sequential Design Phase**: Requirements → Architecture → API/Schema are dependent and gated for human approval before implementation can begin
2. **Parallel Development**: Backend and frontend implementation execute in parallel once API contract is approved
3. **Test-Alongside**: Testing phase overlaps with final implementation polish
4. **Synchronization Points**: Release readiness requires all parallel work (testing, security, documentation) to complete

### Evidence Trail
- **Requirements Node**: Produced 5 functional requirements (FR-01–FR-05), 4 ambiguities marked for approval
- **Architecture Node**: Produced 4 ADRs covering orchestration, API contract, storage, security baseline
- **Implementation Node**: Produced 3 endpoints, 4 backend modules, 2 frontend components
- **Testing Node**: Produced 26 passing tests, 82% line coverage, 75% branch coverage
- **Documentation Node**: Produced operational runbook, review checklist, API documentation
- **Release Gate**: All critical requirements met, approved for production

### Trade-offs Made
- API authentication deferred to brownfield (marked as AMB-01)
- Rate limiting set to simple defaults pending SLO approval (AMB-05)
- PII handling follows aggregate-only analytics by default

---

## Brownfield Scenario: Enhancing Existing System

### Objective
Add expiration and owner-scoped analytics to an existing URL shortener without breaking existing functionality.

### Change Analysis
1. **Inventory Phase**
   - Existing schema: `short_url(id, code, destination_url, created_at, status)`
   - Missing: `expires_at` column, owner attribution, authorization
   - Existing tests: 6 (all focused on core create/redirect)
   - Impact surface: Domain model, service layer, persistence, API contract, frontend

2. **Impact Assessment**
   - **High Risk**: Schema migration (adds nullable `expires_at` column), authorization boundary (analytics ownership)
   - **Medium Risk**: Service-layer date logic (expiry evaluation), API response changes
   - **Low Risk**: Frontend UI updates (add expiry input field)

3. **Migration Plan**
   - Add nullable `expires_at` column to `short_url` table
   - Add `owner_key` column for analytics access control (UUID or opaque string)
   - Update domain model to reflect new fields
   - Extend API request: add optional `expiresAt` and `ownerKey`
   - Extend API response: echo back `expiresAt`
   - Update redirect logic: check expiry before allowing redirect
   - Update analytics endpoint: require `ownerKey` query parameter, validate ownership

4. **Rollback Plan**
   - If migration fails: rollback `ALTER TABLE` statements, restore previous schema
   - If authorization fails: fallback to public analytics (existing behavior)
   - If tests fail: revert domain model changes, re-execute test suite

### Orchestration Workflow
```
brownfield_analysis (READY)
  ├─→ [inventory & impact assessment]
  ├─→ [produce migration & rollback plan]
  └─→ [gate: brownfield_impact_approval] (AWAITING_APPROVAL)
       ↓ [approval granted]
       └─→ implementation (READY)
            ├─→ [execute migration on test database]
            ├─→ [update domain model]
            ├─→ [update service layer]
            ├─→ [update API contract]
            └─→ [update frontend]
                 ↓
                 └─→ testing (READY)
                      ├─→ [backward compatibility test: old URLs still work]
                      ├─→ [new feature test: expiry enforced]
                      ├─→ [new feature test: ownership validated]
                      ├─→ [regression test: all old tests still pass]
                      └─→ [gate: brownfield_validation_pass] (AWAITING_APPROVAL)
                           ↓ [tests approved]
                           └─→ release_readiness (READY)
```

### Evidence Trail
- **Brownfield Analysis**: Identified 3 schema changes, 2 API changes, 1 authorization boundary
- **Implementation**: Updated 5 modules, added 4 new tests for expiry/ownership logic
- **Testing**: All 6 original tests still pass, 4 new scenarios passing
- **Backward Compatibility**: Old URLs without `expiresAt` treated as never-expiring (default)

### Risk Mitigations
- Migration is non-destructive (nullable columns only)
- Rollback is single `ALTER TABLE` per change
- Feature flags (commented out authorization checks) allow gradual rollout
- Monitoring hooks added to track expired URL access attempts

---

## Ambiguous Requirement Scenario: Owner-Scoped Analytics

### Objective
Resolve ambiguity AMB-03: What does "analytics" mean?

### Ambiguity Registration
**Original Requirement**: "Expose analytics for a shortened URL."

**Unresolved Questions**:
1. Does "analytics" mean total clicks, or granular event-level data?
2. Who can access analytics: only the URL creator, or any user?
3. What retention period: 30 days, 1 year, permanent?
4. Real-time or batch? Exact-once or at-least-once semantics?
5. Should we expose user-identifying data (IP, user-agent)?

### Ambiguity Resolution Workflow
```
ambiguity (AWAITING_APPROVAL)
  ├─→ [enumerate options]
  │    ├─→ Option A: Aggregate-only, owner-scoped, 90-day retention (LOW RISK)
  │    ├─→ Option B: Event-level, world-readable, permanent (HIGH PRIVACY RISK)
  │    └─→ Option C: Hybrid with audit log (MEDIUM COMPLEXITY)
  │
  └─→ [select reversible default]
       └─→ Select Option A: aggregate-only, owner-scoped, 90-day retention
            ├─→ [reason: aligns with privacy minimization, simplest to implement]
            ├─→ [reversible: we can upgrade to event-level analytics later]
            └─→ [gate: ambiguity_approval] (AWAITING_HUMAN_APPROVAL)
                 ↓ [product owner approves]
                 └─→ architecture (READY)
                      ├─→ [update ADR-002 (API Contract)]
                      │    ├─→ Add GET /api/v1/urls/{code}/analytics response:
                      │    │    {code, totalClicks, from, to, ownerKey}
                      │    └─→ Add query parameter: ?ownerKey=<string>
                      │
                      └─→ [document in requirements traceability]
                           └─→ FR-03: "Analytics accessible by owner only, aggregate-only for first release"
```

### Decision Record
- **Decision**: Aggregate-only, owner-scoped analytics (Option A)
- **Rationale**: Minimizes privacy risk, simplest to implement and audit, allows future upgrade
- **Trade-off**: No event-level analytics in MVP; suitable for brownfield enhancement
- **Approval Gate**: Product Owner approved 2026-07-30
- **Implementation Constraint**: Add `ownerKey` as opaque identifier; validate on each analytics request

### Evidence Trail
- **Ambiguity Node**: Enumerated 3 options, assessed privacy and complexity trade-offs
- **Decision**: Recorded in ADR-002 (API Contract), traced to FR-03
- **Validation**: Owner-key validation tests added (T-06 scenario)

### Forward Compatibility
- If product owner decides to add event-level analytics later:
  1. Add new endpoint: `GET /api/v1/urls/{code}/events?ownerKey=...`
  2. Existing `/analytics` endpoint remains unchanged
  3. No breaking changes to existing clients

---

## Orchestration Patterns Demonstrated

### 1. Dependency Enforcement
- No node can start until its dependencies complete
- Testing cannot run until implementation is ready
- Release cannot proceed until testing, security, and documentation all pass

### 2. Approval Gates
- Architecture decisions (ADRs) require engineering lead approval
- API contract requires API owner approval
- Ambiguity resolution requires product owner approval
- Security controls require security reviewer approval
- Release readiness requires release manager approval

### 3. Parallel Execution
- Backend and frontend implementation run in parallel once API is approved
- Testing, security, performance, reliability, and documentation can run in parallel after implementation

### 4. State Transitions with Audit Trail
- Every node transition (PENDING → READY → RUNNING → SUCCEEDED) is recorded with timestamp and actor
- Approvals are recorded with approver identity and timestamp
- Failures and retries are logged with reason and outcome

### 5. Failure Handling
- Retry: Up to 3 attempts per node before marking failed
- Invalidation: Failed nodes mark all dependents as blocked
- Rollback: Scoped reversal of implementation changes with validation
- Safe-stop: Critical security or compliance violations halt the entire workflow

### 6. Policy Enforcement
- Schema migrations require change control approval
- API contract changes require backward compatibility verification
- Security findings must be resolved or explicitly approved
- No release without passing all critical gates
