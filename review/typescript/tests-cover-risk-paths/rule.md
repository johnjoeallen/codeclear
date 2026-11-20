# typescript/tests-cover-risk-paths: Tests miss the changed failure, concurrency or contract risk

| Field | Value |
|---|---|
| Applies to | `**/*.test.ts`, `**/*.spec.ts`, `**/*.test.js`, `**/*.spec.js` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Test evidence; node-bug-hunter: Evidence and tests |

## Intent
Supplied tests should reproduce the trigger of a material changed risk and assert the observable bad outcome: failure, concurrent requests, rollback, cancellation, redelivery, authorization, contract compatibility.

## Why AI
Test adequacy depends on which risks the change introduces. Unseen tests are unknown, not absent.

## AI prompt
"List the material risks of this change. For each, do the supplied tests trigger it and assert the visible wrong outcome? Report only a material gap under the main reviewer's finding rules, and only for tests actually supplied." Evidence: diff, supplied tests, the risk list.

## Do not flag
- Tests that were not supplied.
- Risks that the change does not touch.

## Examples
Flag: a new transaction with rollback logic and tests covering only the success path.
OK: test forces the second query to fail and asserts no row persisted.

## Evaluation cases
1. New rollback code, only happy-path test supplied: flag.
2. Concurrency fix with a test running two overlapping calls and asserting one effect: ok.
3. Tests not provided in context: ok (unknown).
