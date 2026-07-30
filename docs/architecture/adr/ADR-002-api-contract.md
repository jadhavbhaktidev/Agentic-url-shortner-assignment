# ADR-002: Versioned REST API

**Status:** Proposed — public API approval required.

Expose versioned JSON APIs under `/api/v1` and isolate public redirect routes. The OpenAPI document is the contract source of truth; incompatible changes require a new version or explicit breaking-change approval.
