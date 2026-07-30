# Decision Lineage

| Decision ID | Agent | Inputs | Outcome | Human approval | Repository reference |
|---|---|---|---|---|---|
| DEC-001 | Requirements/Ambiguity | Assignment | Normalize URL lifecycle + controlled orchestration scope. | Pending APR-001 | requirements model |
| DEC-002 | Architecture | DEC-001, tech stack | Modular Spring Boot + Angular design; control plane separate from runtime. | Pending APR-002 | architecture design |
| DEC-003 | API/Schema | DEC-002 | Versioned REST proposal and relational schema. | Pending APR-003 | OpenAPI/schema |
| DEC-004 | Security | DEC-002/003 | Strict redirect validation and privacy-minimized analytics baseline. | Pending APR-004 | ADR-004 |

Every future decision must include immutable input/output artifact IDs, rationale, actor, approval outcome, timestamp, and repository commit when available.
