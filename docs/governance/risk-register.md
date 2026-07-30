# Risk Register

| ID | Risk | Severity | Probability | Mitigation | Contingency | Owner | Status |
|---|---|---:|---:|---|---|---|---|
| R-001 | Workspace/repository root is ambiguous and uncommitted. | High | High | Reconcile root, remote, branch, and initial commit before code. | Safe stop and human designation. | Repository manager | Open |
| R-002 | Open redirect or SSRF through target URL. | Critical | Medium | Scheme/IP/DNS policy, input validation, security tests, rate limits. | Disable endpoint/revert; incident process. | Security | Open |
| R-003 | Analytics collects excessive personal data. | High | Medium | Aggregate/minimize fields, retention and access policy. | Stop collection, purge under approved process. | Security/data | Open |
| R-004 | Alias collision or concurrency integrity failure. | Medium | Medium | Unique constraint, idempotency, bounded collision retry. | Corrective migration/data repair. | Backend | Open |
| R-005 | Changed upstream design leaves stale agent outputs. | High | Medium | Version artifacts and invalidate DAG descendants. | Replan/revalidate before release. | Orchestrator | Open |
| R-006 | Scope exceeds assignment timebox. | High | High | Vertical slice first, explicit deferred controls. | Reduced scope needs human approval. | Program manager | Open |
