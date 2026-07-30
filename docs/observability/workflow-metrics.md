# Workflow Observability Model

Emit one event for node queued, started, validated, succeeded, failed, retried, fallback, invalidated, rolled back, safe-stopped, approval requested, and approval decided. Correlate with `runId`, node ID, agent ID, artifact IDs/hashes, and commit SHA.

| Metric | Formula | Target / use |
|---|---|---|
| Workflow success rate | successful runs / completed runs | delivery health |
| Agent success rate | successful node attempts / all attempts | agent reliability |
| Retry/fallback/rollback frequency | event count / node attempts | instability signal |
| MTTR | restore time after failed node | recovery effectiveness |
| Approval wait time | approval decision − request | governance latency |
| Validation latency | validation end − start | gate capacity |
| End-to-end delivery time | final state − run creation | program throughput |
| Stale-artifact invalidations | invalidated descendants / changes | replanning health |

Do not include destination URLs, credentials, raw IP addresses, or user-identifying analytics in workflow telemetry.
