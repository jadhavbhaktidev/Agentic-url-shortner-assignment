# Recovery, Retry, Fallback, Rollback, and Safe Stop

| Control | Trigger | Owner | Procedure | Validator |
|---|---|---|---|---|
| Retry | transient agent/build/validation failure | node owner | Record reason and input hashes; retry same bounded task up to 3 times. | Required validation passes and output hash is recorded. |
| Fallback | third retry fails | replanning agent | Choose alternative agent, approved reduced scope, or human escalation. | Governance confirms scope/lineage. |
| Rollback | bad migration, compatibility regression, security regression | change owner | Revert scoped branch/artifact; use approved forward corrective migration or restore process. | Create/redirect/analytics/workflow smoke tests. |
| Safe stop | critical security/compliance risk, unknown root, missing approval, failed rollback | governance agent | Set `SAFE_STOPPED`, checkpoint state, block descendants, notify human owner. | Human records resume decision and validation plan. |

Resume requires an immutable checkpoint reference, root/branch verification, approved remediation, and a new transition event. Never erase failed attempts or prior artifacts.
