# Requirements Model

## Business goal

Deliver a runnable URL shortener prototype that demonstrates a production-oriented Spring Boot + Angular system and a governed, stateful agentic SDLC orchestration layer.

## Functional scope

| ID | Requirement | Acceptance evidence |
|---|---|---|
| FR-01 | Create a short URL from a valid destination URL. | `POST /api/v1/urls` contract and integration test. |
| FR-02 | Redirect a short code to its active destination and record a click. | Redirect integration test and persisted analytics event. |
| FR-03 | Expose analytics for a shortened URL. | Authenticated/owner-scoped analytics API and UI view. |
| FR-04 | Support a greenfield build, brownfield change analysis, and ambiguous-requirement handling. | Scenario reports and decision records. |
| FR-05 | Coordinate SDLC work through an explicit dependency graph with state, retries, approvals, recovery, traceability, and replanning. | Versioned DAG, state ledger, audit artifacts, and validation report. |

## Non-functional requirements

- Java/Spring Boot backend and Angular frontend.
- Modular, testable, secure, reliable, and maintainable delivery.
- Human approval for architecture, public contracts/schema, security controls, breaking changes, production readiness, and release.
- Audit-grade history for decisions, state transitions, validations, and recovery.

## Acceptance criteria traceability

`FR-01`–`FR-03` map to API/schema and test nodes; `FR-04` maps to `docs/requirements/scenario-reports.md`; `FR-05` maps to `execution/workflow/execution-dag.yaml`, state ledger, governance, observability, and traceability artifacts.
