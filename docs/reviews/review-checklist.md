# Engineering Review and Release Checklist

## Review contract

Each child agent or change author provides: objective, inputs and dependencies, tasks executed, artifacts, validation results, risks, state/repository updates, proposed commit, PR summary, approvals, rollback information, observability metrics, and execution duration. Missing evidence blocks the relevant quality gate.

PRs use `<type>(scope): <summary>` commits and include background, changes, files modified, validation, risks, rollback plan, approvals required, and merge readiness. Link all claims to a commit or repository artifact.

## Architecture and contract review

- [ ] Requirements, assumptions, and ambiguity decisions remain aligned with the approved scope: [requirements model](../requirements/requirements-model.md) and [assumptions](../requirements/ambiguity-and-assumptions.md).
- [ ] Architecture, service boundaries, data flow, and all relevant ADRs have recorded approval: [architecture design](../architecture/architecture-design.md).
- [ ] Public API is versioned, documented, compatible, and approved: [OpenAPI contract](../../openapi/openapi.yaml).
- [ ] Schema/migration includes constraints, ownership, backup/rollback or forward-fix approach, and compatibility evidence.
- [ ] Upstream changes have invalidated and revalidated affected DAG descendants: [workflow DAG](../../execution/workflow/execution-dag.yaml).

## Implementation and validation review

- [ ] Source changes are limited to the reviewed task and configuration contains no secrets.
- [ ] Unit, integration, and E2E tests cover URL creation, URL validation, redirects, error behavior, expiry/disable behavior, analytics, and authorization where applicable.
- [ ] Build and tests are reproducible from documented commands; results, coverage, artifacts, and versions are attached.
- [ ] Accessibility, browser compatibility, API error handling, and observable logs/metrics meet the accepted scope.
- [ ] Performance and reliability evidence covers expected load, redirects, database constraints, collision handling, health/readiness, and failure recovery.

## Security and privacy review

- [ ] Destination URL policy permits only approved schemes and addresses open redirect, SSRF, DNS/IP bypass, and malformed URL risks.
- [ ] Creation and redirect abuse controls (rate limits, logging, and alerting) are tested.
- [ ] Secrets use the approved secret store; authentication/authorization and CORS assumptions are documented.
- [ ] Analytics is minimized, access-controlled, retained only as approved, and excludes unapproved personal data.
- [ ] Findings, mitigations, residual risks, and security approval are recorded against [ADR-004](../architecture/adr/ADR-004-security-baseline.md).

## Release readiness and rollback review

- [ ] Release owner confirms required human approvals in the [approval matrix](../governance/approval-matrix.md).
- [ ] Deployment plan identifies artifacts, version/commit, environment configuration, database migration, smoke tests, monitoring, and owner.
- [ ] Rollback trigger, scope, procedure, validator, and owner are viable and rehearsed: [operational runbook](../runbooks/operational-runbook.md).
- [ ] Critical security/compliance risk, missing approval, unsafe output, or failed rollback causes a recoverable safe stop under the [recovery policy](../../execution/recovery/recovery-runbook.md).
- [ ] Open risks are accepted by the accountable human owner or release is blocked: [risk register](../governance/risk-register.md).

## Audit, traceability, and final signoff

- [ ] Workflow status and node transitions are persisted in the state record and every artifact is version-controlled.
- [ ] Decision IDs connect input, output, rationale, approval, and repository reference: [decision lineage](../traceability/decision-lineage.md).
- [ ] Validation history, retries, fallbacks, rollbacks, and agent durations are captured in observability evidence.
- [ ] Reviewers verify no stale output was used after a requirements, architecture, API, or schema change.
- [ ] Final signoff records approver, timestamp, scope, release version, residual risks, and release/hold decision.

## Required review outcome

Select one and add a concise rationale: `APPROVED`, `APPROVED WITH FOLLOW-UPS`, `CHANGES REQUESTED`, or `BLOCKED / SAFE_STOPPED`. A release requires explicit production-readiness and release approval; agents cannot self-approve protected gates.
