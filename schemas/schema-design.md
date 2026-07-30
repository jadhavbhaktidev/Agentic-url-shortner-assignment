# Proposed Schema

| Table | Key fields | Controls |
|---|---|---|
| `short_url` | `id`, `code`, `destination_url`, `owner_id`, `created_at`, `expires_at`, `status` | unique `code`, URL length bound, lifecycle index |
| `click_aggregate` | `short_url_id`, `bucket_start`, `click_count` | composite unique key; aggregated privacy-preserving data |
| `workflow_execution` | `execution_id`, `status`, `dag_version`, `updated_at` | append-only transition history |
| `workflow_node_attempt` | `node_id`, `attempt`, `status`, `artifact_refs`, `decision_refs` | max 3 retry policy |

Migration rollback uses a forward corrective migration; production restore requires the approved backup/runbook process. Schema changes are blocked pending approval.
