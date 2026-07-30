#!/bin/bash
# documentation-agent

## Purpose
Produces operational and governance documentation.
Maintains consistency between code, design, and deployment guides.

## Interface
Input: implementation artifacts, design decisions, test results
Output: runbooks, checklists, architecture diagrams, API documentation

## Responsibilities
1. Generate/update API documentation from OpenAPI contract
2. Produce operational runbook (setup, deployment, troubleshooting)
3. Produce review checklist (code, security, compliance)
4. Document architecture with decision rationale
5. Record deployment and monitoring guidance

## Exit condition
- Operational runbook exists and is current
- Review checklist covers critical paths
- API documentation matches implementation
- Documentation artifact recorded in execution state
