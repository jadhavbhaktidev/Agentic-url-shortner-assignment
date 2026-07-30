# URL Shortener Operational Runbook

## Purpose and scope

This runbook is the operational handoff skeleton for the Spring Boot API, Angular web application, PostgreSQL datastore, and their governed delivery workflow. Complete the bracketed environment-specific values during implementation; no deployment is authorized by this document alone.

## Service ownership and escalation

| Area | Accountable owner | Evidence / handoff |
|---|---|---|
| Runtime API and redirects | Backend owner | API contract: [OpenAPI](../../openapi/openapi.yaml) |
| Web UI | Frontend owner | Build, browser, and E2E evidence |
| Database and migrations | Data owner | Approved schema/migration and restore record |
| Security and privacy | Security reviewer | [Security ADR](../architecture/adr/ADR-004-security-baseline.md) and findings |
| Delivery workflow | Release owner | [Workflow DAG](../../execution/workflow/execution-dag.yaml) and state checkpoint |

## Setup and configuration checklist

1. Record the approved deployment environment, base URL, runtime versions, and repository commit: `[TBD]`.
2. Provision PostgreSQL with least-privilege application credentials; store credentials in the approved secret manager, never source control.
3. Configure allowed destination URL policy, rate limits, CORS origins, logging/metric sinks, and retention settings from the approved security baseline.
4. Run the documented Spring Boot and Angular build commands after the implementation agent provides them. Capture dependency lockfiles and build artifact hashes.
5. Execute database migration in a non-production environment first; attach validation evidence and an approved rollback/forward-fix plan.

## Deployment flow

1. Confirm the branch, commit, clean CI status, and required approvals in the [approval matrix](../governance/approval-matrix.md).
2. Deploy an immutable API artifact and matching Angular artifact to `[TBD environment]`.
3. Apply the approved database migration exactly once and record migration version/checksum.
4. Run smoke checks: health/readiness, create URL, valid redirect, unknown/expired redirect handling, and aggregate analytics authorization.
5. Monitor error rate, redirect latency, collision rate, rate-limit events, database health, and workflow validation latency for `[TBD observation window]`.
6. Record release evidence, commit, environment, operator, timestamps, and decision IDs in the release-readiness report.

## Incident response and safe stop

Trigger an incident for open-redirect/SSRF indications, exposed secrets, unexplained redirect failures, corruption, privacy breach, failed migration, or critical availability degradation.

1. Preserve logs, request/correlation IDs, deployment version, workflow checkpoint, and scope; do not alter prior audit artifacts.
2. Contain impact: disable affected create/redirect capability, tighten rate limits, or remove the exposed artifact as approved by the incident owner.
3. Set workflow state to `SAFE_STOPPED`, block dependent nodes, and notify the release owner and security reviewer for security/privacy incidents.
4. Diagnose and document the cause, affected data, mitigation, recovery test, and residual risk.
5. Resume only with an immutable checkpoint, approved remediation, validation plan, and recorded human decision, per the [recovery policy](../../execution/recovery/recovery-runbook.md).

## Rollback and recovery

| Trigger | Immediate action | Recovery validation | Owner |
|---|---|---|---|
| Bad application release | Revert to prior immutable artifact | Health, create, redirect, analytics smoke tests | Runtime owner |
| Migration or data integrity regression | Halt rollout; use approved forward corrective migration or restore process | Schema version, data integrity checks, application smoke tests | Data owner |
| Security/privacy regression | Disable risky path and revoke/rotate affected secrets | Security retest and reviewer approval | Security owner |
| Workflow or approval inconsistency | Checkpoint and safe-stop affected DAG descendants | State/lineage and gate reconciliation | Workflow owner |

Rollback must be scoped to the affected artifact/branch, recorded with rationale and timestamps, and validated before further rollout. Follow the authoritative [recovery runbook](../../execution/recovery/recovery-runbook.md) for retry limits and fallback order.

## API and documentation maintenance

- The API owner updates [OpenAPI](../../openapi/openapi.yaml) with every contract change and obtains API/data-owner approval for breaking changes or schema migrations.
- The architecture owner maintains [architecture design](../architecture/architecture-design.md) and ADRs.
- The documentation owner updates this runbook, setup material, API guidance, and release notes using implementation and validation evidence.
- Every material decision links to [decision lineage](../traceability/decision-lineage.md), its commit, validation, approval, and rollback impact.

## Operational risks

See the authoritative [risk register](../governance/risk-register.md). Highest-priority operational concerns are unsafe destination validation, data/privacy overcollection, alias collisions, stale workflow artifacts, and an unclear repository/runtime baseline.

## Completion evidence

Before marking documentation complete, attach setup commands, configuration keys (without values), deployment steps, actual monitoring dashboard links, on-call contacts, recovery timing objective, test results, and release approval reference. Replace all `[TBD]` values with reviewed production facts.
