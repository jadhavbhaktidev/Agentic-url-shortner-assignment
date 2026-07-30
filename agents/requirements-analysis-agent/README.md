#!/bin/bash
# requirements-analysis-agent

## Purpose
Interprets assignment requirements and produces a normalized requirements model.
Validates interpretations against assignment brief and marks ambiguities for resolution.

## Interface
Input: assignment brief (plain text or markdown)
Output: requirements-model.md with FR-01 through FR-05 and acceptance criteria

## Responsibilities
1. Parse assignment scope (greenfield URL shortener)
2. Normalize into functional and non-functional requirements
3. Identify ambiguities (auth, rate limiting, PII handling, SLO)
4. Map requirements to acceptance criteria
5. Produce docs/requirements/requirements-model.md

## Exit condition
Requirements model exists and is marked VALIDATED in workflow state.
