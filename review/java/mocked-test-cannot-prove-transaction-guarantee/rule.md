# java/mocked-test-cannot-prove-transaction-guarantee: Mocks used to claim transactional delivery

| Field | Value |
|---|---|
| Applies to | `src/test/**/*.java` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Test evidence |

## Intent
Mocked repositories and brokers cannot prove commit/publish ordering. When the MR claims an atomic or ordered delivery guarantee, mock-only tests do not substantiate it.

## Why AI
It requires linking an MR claim (description, comments, javadoc) to what the tests can actually demonstrate.

## AI prompt
"Does the MR claim a transactional or ordering guarantee (outbox, after-commit publish, exactly-once)? If so, do the supplied tests use a real transaction manager/broker (Testcontainers, embedded) or only mocks? Report only if the guarantee is claimed and mock-only tests are the sole evidence." Evidence: MR description, changed code and comments, test files.

## Do not flag
- MRs that make no such claim.
- Unit tests that are not offered as proof of the guarantee.

## Examples
Flag: "publishes after commit" claimed; test mocks `KafkaTemplate` and `TransactionTemplate`.
OK: integration test with embedded Kafka and a rolled-back transaction asserting nothing is published.

## Evaluation cases
1. Claim of exactly-once, only Mockito tests - report.
2. Claim backed by Testcontainers rollback test - no report.
3. No guarantee claim - no report.
