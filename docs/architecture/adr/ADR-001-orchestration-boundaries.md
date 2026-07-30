# ADR-001: Governed stateful orchestration

**Status:** Proposed — human approval required.

Use a version-controlled DAG and durable state ledger to govern specialized agents. Nodes run only when dependencies and approval gates pass. Each attempt is bounded to three retries and produces lineage, validation, and recovery evidence. This provides reproducibility without allowing autonomous deployment or irreversible changes.
