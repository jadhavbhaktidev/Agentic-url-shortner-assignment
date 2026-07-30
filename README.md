# Agentic URL Shortener — SDLC Control Plane

This workspace contains the governed, repository-first execution package for the URL Shortener assignment. It deliberately contains no application implementation: implementation is blocked until the architecture, public API/schema, and security-control approval gates are accepted by a human owner.

The assignment source is `Interview Assignment Build an Agent.txt`.

## Current status

`PLANNED_WITH_APPROVAL_GATES` — requirements have been normalized; the workflow DAG, state ledger, decision lineage, and governance controls are ready. The Git repository has no commits and this workspace currently contains only this control-plane package and the assignment. Git also reports unrelated pre-existing changes in a sibling directory; they are out of scope and were not modified.

## Navigation

- Requirements and ambiguity decisions: `docs/requirements/`
- System/API/data design: `docs/architecture/`, `openapi/`, `schemas/`
- Governed workflow/state/recovery: `execution/`
- Policy, risk, approvals, and review: `docs/governance/`, `docs/reviews/`
- Metrics and decision audit trail: `docs/observability/`, `docs/traceability/`

## Human action required

Approve decisions `ADR-001` through `ADR-004` and the public API/schema contract. Then designate the repository containing the Spring Boot/Angular implementation. This releases the bounded development nodes defined in the execution DAG.

## Proposed delivery sequence

1. Approve architecture, API/schema, and security baseline.
2. Execute Spring Boot and Angular development under the DAG.
3. Run unit, integration, E2E, security, performance, reliability, documentation, governance, and audit gates.
4. Review release-readiness evidence and approve release.
