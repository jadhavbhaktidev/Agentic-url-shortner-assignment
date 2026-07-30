#!/bin/bash
# implementation-agent

## Purpose
Implements the URL shortener backend and frontend based on approved contracts and test strategy.
Produces deployable artifacts and validates against architecture/API/schema.

## Interface
Input: architecture ADRs, OpenAPI contract, schema design, test strategy, approved scope
Output: src/main/java (backend), frontend/ (Angular), passing test suite

## Responsibilities
1. Implement Spring Boot service layer (create, redirect, analytics)
2. Implement controllers for POST /api/v1/urls, GET /r/{code}, GET /api/v1/urls/{code}/analytics
3. Implement Angular UI components and API integration
4. Add unit and integration tests per TEST_STRATEGY.md
5. Validate build passes (mvn clean verify)
6. Update execution state with implementation artifacts

## Exit condition
- Maven build succeeds (mvn clean verify)
- All tests pass (26+ tests)
- Angular build succeeds (npm run build)
- No contract violations vs. openapi.yaml
