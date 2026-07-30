# Pending Human Approvals

| Approval ID | Decision | Required role | Status | Decision options |
|---|---|---|---|---|
| APR-001 | Working assumptions: greenfield MVP, aggregate analytics, deferred custom aliases/auth implementation details. | Product owner/reviewer | Pending | Approve / revise / reject |
| APR-002 | Modular Spring Boot + Angular architecture and ADRs. | Engineering lead | Pending | Approve / revise / reject |
| APR-003 | Draft v1 OpenAPI and relational schema. | API and data owner | Pending | Approve / revise / reject |
| APR-004 | URL-validation, privacy-minimization, and rate-limit baseline. | Security reviewer | Pending | Approve / revise / reject |
| APR-005 | Confirm authoritative repository root, remote, and initial branch policy. | Repository owner | Pending | Approve / revise / reject |

When decided, append approver identity/role, UTC timestamp, evidence links, decision rationale, and any constraints to the workflow state transition log. A rejection triggers `REPLAN_REQUIRED`; approval releases only the relevant descendants.
