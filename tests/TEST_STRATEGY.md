# URL Shortener Test Strategy

**Workflow node:** `test_prep`  
**Run:** `wf-20260730-001`  
**Status:** prepared; executable suites remain blocked until an implementation artifact exists.

## Objective and governed inputs

Define the verification approach for the approved URL-shortener baseline. This strategy traces to `FR-01` through `FR-05`, the approved architecture/ADRs, `openapi/openapi.yaml`, `schemas/schema-design.md`, the dependency graph, and the risk register. It is a preparation artifact only: no application code, test executable, or test result is claimed by this node.

## Test matrix

| Layer | Coverage focus | Primary evidence | Entry / exit criteria |
|---|---|---|---|
| Unit | URL normalization, HTTP(S)-only validation, code generation/collision retry, lifecycle status, analytics aggregation, error mapping | JUnit tests and coverage report | Enter after backend modules exist; exit when deterministic business logic and boundary cases pass. |
| Integration | `POST /api/v1/urls`, `GET /r/{code}`, `GET /api/v1/urls/{code}/analytics`; persistence constraints; migration behavior | Spring Boot integration tests against isolated PostgreSQL-compatible database | Enter after API, migration, and repository layers build; exit when contract responses and database assertions pass. |
| Contract | Request/response/status/header compatibility with `openapi/openapi.yaml`, including redirect `Location` header | OpenAPI validation and consumer-oriented contract tests | Enter after controllers exist; exit when no unapproved contract drift exists. |
| E2E | Angular creation flow, short-link navigation, owner-scoped analytics display, recoverable error states | Browser E2E recordings/results against deployed local test stack | Enter after backend + Angular vertical slice deploys; exit when critical user journeys pass. |
| Security | Malformed/non-HTTP URL rejection, open-redirect control, authorization boundary for analytics, rate-limit behavior, sensitive-data log review, dependency scanning | Security test report and triaged findings | Enter after implementation; exit when no unresolved critical/high finding or approval-backed exception exists. |
| Reliability | Duplicate-code contention, inactive/expired/missing code handling, database interruption behavior, retry limits, health/readiness, recoverable workflow state | Failure-injection report and recovery evidence | Enter after deployment configuration exists; exit when recovery and no-data-corruption criteria pass. |
| Performance | Create/redirect/analytics baseline under approved workload; latency and error-rate profile | Benchmark report | Blocked until product owner defines SLO/load profile (AMB-05). |
| Orchestration | DAG dependency enforcement, approval gates, retry maximum of three, safe-stop and descendant invalidation | State-transition and audit evidence | Execute throughout workflow; exit when `FR-05` lineage is complete. |

## Required scenario set and test data

Use generated, non-production URLs under reserved example domains. Each fixture must be isolated and cleaned through migrations/transactions or ephemeral databases.

| ID | Scenario / fixture | Expected result |
|---|---|---|
| T-01 | Valid HTTPS destination | `201` response with code, short URL, and creation time. |
| T-02 | Invalid scheme, malformed URL, oversized input | `400`; no short URL persisted. |
| T-03 | Deliberate code collision | Unique constraint remains intact; bounded retry produces unique code or controlled error. |
| T-04 | Active code with click | `302` plus correct `Location`; one aggregate click recorded exactly per approved consistency model. |
| T-05 | Unknown, disabled, and expired codes | `404`; no successful redirect. |
| T-06 | Analytics for owner and non-owner | Owner receives aggregate; non-owner behavior follows approved owner-key mechanism without disclosure. |
| T-07 | Time-window analytics boundary | Inclusive/exclusive semantics documented and aggregate remains accurate. |
| T-08 | Rate-limit threshold / burst | `429` and observability signal; normal traffic recovers after window. |
| T-09 | Database unavailable during create/redirect | Defined failure response, health/readiness impact, no partial corruption. |
| T-10 | Requirement/ADR/API change | Descendant test results marked stale and re-executed under DAG invalidation policy. |

No production URLs, credentials, personal data, browser identifiers, or unminimized IP/user-agent values may be used in fixtures or test output.

## Quality gates and coverage policy

- All critical-path unit, integration, contract, and E2E tests must pass before release readiness.
- New or changed backend business logic requires at least 80% line coverage and 70% branch coverage at the affected module boundary; generated/configuration code may be excluded only with review evidence.
- Creation, redirect, analytics authorization, validation, and persistence migration paths require explicit tests regardless of aggregate coverage.
- Contract drift is release-blocking unless the OpenAPI change is approved and all affected consumers/tests are revalidated.
- No unresolved critical/high security finding, data-integrity failure, or failed rollback/recovery exercise may pass the release gate.
- Performance gate has no numeric threshold until AMB-05 is resolved; a production-readiness decision cannot treat it as passed beforehand.

## Execution, failure, and re-execution rules

Testing executes only after `implementation` and this `test_prep` node have succeeded. Security, performance, reliability, documentation, and brownfield nodes synchronize independently before release readiness.

On a failure, preserve logs, inputs, environment/version identifiers, result artifacts, and the affected requirement/decision IDs. Return defects to the implementation owner, rerun the smallest affected suite, then rerun all dependent regression/contract tests. Each agent may retry a failed execution at most three times, recording reason and outcome. After that, apply the workflow fallback order: alternative agent, approved reduced scope, human escalation, then safe halt. An approved requirement, ADR, API, or schema change invalidates descendant test artifacts; retain them for audit but never use them to satisfy the quality gate.

Rollback triggers include data corruption, unsafe redirect behavior, authorization leakage, an irrecoverable migration defect, or a test-induced instability outside the isolated environment. Roll back the scoped branch/artifact or use an approved forward corrective migration; the implementation owner performs it, the testing agent validates it with T-01, T-04, T-05, T-06, and workflow-state recovery evidence.

## Risks and controls

| Risk | Control / contingency |
|---|---|
| Authentication remains unspecified (AMB-01) | Keep analytics authorization tests parameterized; block exposure assumptions until mechanism is implemented and reviewed. |
| Redirect analytics consistency is undecided | Assert only approved consistency semantics; require an ADR update before exact-once claims. |
| Privacy leakage in event data | Use aggregate-only assertions and log-scrubbing checks; escalate any PII finding. |
| Unspecified SLO/load profile (AMB-05) | Prepare harness but hold numeric performance signoff pending approval. |
| Shared/non-isolated test state | Use ephemeral databases, unique fixtures, and deterministic clocks where feasible. |

## Child-agent execution contract

The executing testing agent will report: objective; inputs/dependencies; tasks executed; artifacts produced; validation results; risks; workflow/repository state updates; recommended commit; PR title, background, changes, files, validation, risks, rollback, approvals, and merge readiness; rollback trigger/scope/procedure/validator/owner; observability metrics; execution duration; retry and fallback outcomes. Metrics include pass rate, requirement coverage, flaky-test rate, rerun count, validation latency, and mean test duration.

## Repository proposal

- **Recommended commit:** `docs(test): add URL shortener test strategy`
- **Suggested branch:** `docs/test-strategy` (or the active governed execution branch)
- **PR title:** `docs(test): define URL shortener verification strategy`
- **PR summary:** Adds traceable unit, integration, contract, E2E, security, reliability, and performance test preparation. No production or executable test code is changed.
- **Approvals required:** none for this preparation artifact; public contract, security-control, and release approvals remain governing gates for execution.
