#!/bin/bash
# testing-agent

## Purpose
Prepares and executes test strategy. Validates implementation against requirements.
Produces test evidence and coverage reports.

## Interface
Input: TEST_STRATEGY.md, implementation artifacts
Output: test execution report, coverage metrics, passing test suite

## Responsibilities
1. Execute unit tests (T-01 through T-10 scenarios)
2. Validate contract compliance vs openapi.yaml
3. Execute integration tests (create, redirect, analytics flows)
4. Execute E2E tests (Angular UI against backend)
5. Measure coverage (target: 80% line, 70% branch for business logic)
6. Record pass/fail/skip counts
7. Produce test report artifact

## Exit condition
- All critical-path tests pass
- Coverage requirements met
- No unresolved contract drift
- Test report recorded in execution state
