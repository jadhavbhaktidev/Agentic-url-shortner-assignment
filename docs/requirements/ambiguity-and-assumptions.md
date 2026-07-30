# Ambiguity and Assumptions Register

| ID | Open point / assumption | Decision or required approval | Risk |
|---|---|---|---|
| AMB-01 | Authentication/tenancy is unspecified. | Prototype uses an owner key abstraction; authentication mechanism needs product approval before exposure. | Unauthorized analytics access. |
| AMB-02 | Custom aliases, expiration, and deletion are unspecified. | Treat as deferred enhancements; aliases must have collision and reserved-word policy if approved. | Scope creep. |
| AMB-03 | Storage technology is unspecified. | PostgreSQL proposed for transactional URLs and click aggregates; approval required. | Operational mismatch. |
| AMB-04 | Analytics retention and privacy are unspecified. | Store minimized event attributes, aggregate by default, and define retention before production. | Privacy/compliance breach. |
| AMB-05 | Scale/SLO targets are unspecified. | Establish load profile and SLO before performance gate. | Under-designed capacity. |
| AMB-06 | Assignment calls for agent orchestration but names no runtime. | Use a version-controlled workflow state model; any runtime framework is implementation-phase decision. | Overengineering. |

### Interpretation

The differentiator is controlled, non-linear orchestration—not simply generating a URL API. No ambiguous item may silently become a public contract or production control; its state remains `PENDING_HUMAN_DECISION`.
