# python/mock-only-guarantee-tests: Mock-only tests offered as proof of ordering or delivery guarantees

| Field | Value |
|---|---|
| Applies to | `**/test_*.py`, `**/*_test.py` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Test evidence; python-bug-hunter: Evidence and tests |

## Intent
When the MR claims a database/broker ordering or delivery guarantee (commit before ack, exactly-once, transactional outbox), tests that mock the database and broker cannot prove it.

## Why AI
Only applies when the MR/description claims the guarantee; needs the claim and the test doubles used.

## AI prompt
"Does the MR text or code comments claim an ordering/delivery/atomicity guarantee? Do the supplied tests verify it against a real or faithful database/broker, or only against mocks that assume the guarantee?" Supply the MR description, the code and the tests. Report only when a claim exists and the tests are mock-only; missing tests alone are not a defect; otherwise NO_FINDING.

## Do not flag
- No guarantee claimed; integration tests with real containers or in-memory fakes that model the semantics.

## Examples
Flag:
```python
def test_ack_after_commit(): consumer.handle(msg); producer.ack.assert_called_once()   # db and broker mocked
```
OK:
```python
def test_ack_after_commit(pg, rabbit): ... assert row_exists() and not rabbit.unacked()
```

## Evaluation cases
1. MR says "ack only after commit"; tests mock `db` and `channel` -> flag.
2. Same claim, testcontainers integration test -> no finding.
3. No guarantee claimed, unit tests with mocks -> no finding.
4. Claim of exactly-once with a test asserting `send` called once on a mock -> flag.
