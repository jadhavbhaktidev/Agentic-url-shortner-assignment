# Task Decomposition

**Run:** `wf-20260730-001`  
**Owner:** Task Decomposition Agent  
**Status:** complete; implementation work remains governed by the execution DAG.

## Objective and inputs

Decompose the approved URL-shortener requirements into independently executable, dependency-aware work. Inputs are the approved requirements model, architecture decisions, OpenAPI/schema proposal, risk register, and `execution/workflow/execution-dag.yaml`. The authoritative repository is `jadhavbhaktidev/Agentic-url-shortner-assignment.git`.

## Epics, stories, and deliverables

| Epic | Story / task | Primary owner | Deliverables | Depends on |
|---|---|---|---|---|
| E1 Governance baseline | Confirm decisions, state, approvals, and traceability | Governance / Audit | approval entries, decision lineage, state transition | requirements |
| E2 Contracted design | Finalize architecture, API, and persistence contracts | Architecture / API & Schema | ADRs, OpenAPI, schema/ERD | requirements, approved ambiguity |
| E3 Delivery preparation | Prepare test strategy and documentation skeleton | Testing / Documentation | test plan, fixtures, README/runbook outline | requirements |
| E4 URL-shortening vertical slice | Build URL creation, validation, alias generation, persistence, and error handling | Greenfield Development | Spring Boot modules, configuration, migrations | E2, decomposition |
| E5 Redirect and analytics | Build redirect, click capture, analytics query, and Angular views | Implementation | backend endpoints, Angular UI, automation | E4 |
| E6 Verification | Execute unit, integration, contract, and end-to-end tests | Testing | test suites, coverage/report | E3, E5 |
| E7 Non-functional validation | Assess security, performance, and recovery behaviour | Security / Performance / Reliability | threat model, benchmarks, failure/recovery evidence | E5 |
| E8 Release package | Complete operating docs, readiness, governance audit, and final summary | Documentation / Release / Governance / Audit | readiness report, audit, release decision | E5, E6, E7 |

## Dependency mapping and execution plan

```text
requirements
  ├─ ambiguity ───────────┐
  ├─ architecture ── api_schema ──┐
  ├─ decomposition ───────────────┼─ greenfield ── implementation ──┬─ testing
  ├─ test_prep ───────────────────┘                                 ├─ security
  └─ docs_prep ─────────────────────────────────────────────────────┼─ performance
                                                                      ├─ reliability
                                                                      └─ documentation
testing + security + performance + reliability + documentation + brownfield
  └─ release_readiness ── governance_audit ── final_summary
```

`architecture` and `decomposition` may run in parallel after requirements. `test_prep` and `docs_prep` may also run in parallel after requirements. After implementation, testing, security, performance, reliability, documentation, and brownfield analysis run in parallel and synchronize at release readiness. The authoritative machine-readable mapping remains `execution/workflow/execution-dag.yaml`; this plan does not replace it.

## Milestones and quality criteria

| Milestone | Entry conditions | Exit / success criteria | Failure criteria and re-execution |
|---|---|---|---|
| M1 Design baseline | approved requirements; repository confirmed | ambiguity, ADR, API/schema approvals recorded; DAG descendants unblocked | conflict, missing approval, or changed requirement: safe-stop/replan; rerun affected design nodes |
| M2 Buildable vertical slice | M1; development node ready | create, redirect, analytics code and configuration compile; migrations reproducible | build/contract failure: retry owner up to three times, then fallback/escalate |
| M3 Verified system | M2; test preparation available | unit/integration/E2E and contract checks pass; regression evidence stored | test failure or insufficient coverage: return to implementation; rerun impacted tests |
| M4 Operational acceptance | M2 | security, performance, reliability findings triaged or mitigated | critical security/compliance/recovery failure: safe-stop, scoped rollback, revalidate |
| M5 Release decision | M3 + M4 + docs + brownfield review | readiness/governance/audit pass and release approval captured | unresolved high risk or missing approval: hold release and replan |

## Validation mapping

| Requirement | Validation evidence | Gate |
|---|---|---|
| FR-01 Create short URL | API contract plus backend integration test for valid/invalid URL and collision handling | testing |
| FR-02 Redirect/click capture | redirect integration test, click persistence assertion, abuse-control tests | testing, security |
| FR-03 Analytics | owner-scoped analytics contract, integration test, Angular E2E view | testing, security |
| FR-04 Three delivery scenarios | greenfield implementation record; brownfield impact/migration/rollback report; ambiguity register | brownfield, governance |
| FR-05 Governed orchestration | DAG state transitions, approvals, retries/fallback/rollback records, audit report | governance_audit |

## Risks and controls

Use `docs/governance/risk-register.md` as the canonical register. This decomposition assigns R-002/R-003 to security validation, R-004 to greenfield implementation and testing, R-005 to replanning and audit, and R-006 to release-readiness scope control. A critical security, compliance, untrusted-repository, missing-approval, or rollback failure triggers the DAG safe-stop policy.

## Child-agent execution contract

Every executing agent must report: objective; inputs and dependencies; tasks executed; artifacts and repository updates; validation results; risks; state updates; recommended conventional commit; PR title/background/changes/files/validation/risks/rollback/approvals/merge readiness; rollback trigger/scope/procedure/validator/owner; observability metrics; execution duration; retries/fallbacks. Outputs must cite their decision IDs and preserve upstream context.

## Retry, fallback, and rollback

Each task may retry a maximum of three times, recording reason and outcome in workflow state. Then use the approved order: alternative agent, reduced scope with approval, human escalation, safe halt. Roll back only the scoped artifact or branch (or use an approved forward corrective migration); validate creation, redirect, analytics, and recoverable workflow state before resuming.

## Recommended repository change

- **Commit:** `docs(planning): add task decomposition`
- **Branch:** `docs/approve-design-baseline` (or a dedicated `docs/task-decomposition` branch if PR isolation is required)
- **PR title:** `docs(planning): add URL shortener task decomposition`
- **PR summary:** Adds dependency-aware epics, validation mapping, milestones, failure/re-execution rules, and the required child-agent reporting contract. No application source changes.
