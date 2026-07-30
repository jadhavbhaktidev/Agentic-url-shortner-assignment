# Approval Matrix and Quality Gates

| Gate | Required human role | Evidence | Blocks |
|---|---|---|---|
| Requirement interpretation | Product owner/reviewer | Assumption register | API/schema and implementation |
| Architecture/ADR | Engineering lead | ADRs and architecture review | API/schema and implementation |
| Public API/schema/migration | API + data owner | OpenAPI compatibility and migration/rollback plan | implementation |
| Security controls/privacy | Security reviewer | threat assessment and test evidence | release |
| Breaking change/major refactor | Engineering + consumer owner | impact, regression, migration plan | merge |
| Production readiness | Engineering + operations/reviewer | readiness report and rollback rehearsal | release |
| Release | Release owner | complete audit and accepted residual risks | deployment |

Agents may create artifacts and execute non-destructive validation in their assigned scope. They may not self-approve protected gates, deploy, accept high risks, or overwrite audit history.
