# Architecture Design

## Components

`Angular SPA` → `Spring Boot REST API` → `URL Service` → `PostgreSQL`.

The redirect path uses a dedicated redirect controller/service, increments analytics asynchronously or transactionally according to the approved consistency decision, and emits structured logs/metrics. The control plane is separate: workflow DAG + persistent state + agent outputs + approval/decision records drive delivery; it does not make autonomous production changes.

## Service boundaries

- URL lifecycle: validate, normalize, create, find, disable/expire.
- Redirect: resolve active code, issue safe redirect, record outcome.
- Analytics: record and query privacy-minimized aggregate clicks.
- Web UI: create links and display owner-scoped analytics.
- Orchestration: schedule bounded agents, enforce dependencies/gates, retain state and audit lineage.

## Data flow

Create → validate destination → generate collision-resistant code → persist URL → return API response. Redirect → resolve code → reject inactive/missing → record outcome → HTTP redirect. Analytics → query aggregated click data by URL and time window.

## Reliability and security baseline

Validate only HTTP(S) destinations, protect against SSRF if server-side fetches are introduced, rate-limit creation/redirect abuse, use unique code constraints and retry-on-collision, expose health/readiness endpoints, and keep secrets out of source and decision artifacts.
