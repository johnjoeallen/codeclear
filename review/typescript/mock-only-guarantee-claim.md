# typescript/mock-only-guarantee-claim: Mock-only tests cited as proof of database or broker guarantees

| Field | Value |
|---|---|
| Applies to | `**/*.test.ts`, `**/*.spec.ts`, `**/*.test.js`, `**/*.spec.js` |
| Category | TESTING |
| Severity | INFO |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Test evidence; node-bug-hunter: Evidence and tests |

## Intent
Mocks of the DB or broker cannot prove transaction ordering, isolation, rollback, ack timing or delivery guarantees. Note the limitation only when the change claims such a guarantee.

## Why AI
Requires the MR description or ticket's claim and knowledge of what the mock replaces.

## AI prompt
"Does the change or description claim a transaction, ordering, delivery or isolation guarantee? Are the only supplied tests built on mocks of that infrastructure? If both, report one informational note naming the unproven guarantee. Otherwise report nothing." Evidence: MR description/ticket, tests, mock setup.

## Do not flag
- Changes that make no such claim, or tests that use a real or containerized database/broker.

## Examples
Flag: description says "guarantees no double charge under concurrency" and the test mocks the repository.
OK: integration test against Testcontainers Postgres with two concurrent requests.

## Evaluation cases
1. Claim of atomicity, repository mocked: flag (info).
2. No guarantee claimed, mocked unit test: ok.
3. Real DB integration test: ok.
