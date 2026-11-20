# python/test-risk-coverage: Tests omit the material risks of the change

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | TESTING |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Test evidence; python-bug-hunter: Evidence and tests |

## Intent
Tests for a change do not cover its material risks: failures, rollback, cancellation, concurrent requests, redelivery, authorization and contract compatibility, or do not execute the real decorator/async/transaction path. Unseen tests remain unknown.

## Why AI
Test adequacy depends on which risks the change introduces and whether assertions observe behavior.

## AI prompt
"List the material risks the changed code introduces. For each, is there a supplied test that exercises the actual path (decorator, async, transaction) and asserts observable behavior? Which risks have no visible test?" Supply the diff, test files and the MR description. Report a test gap only for a concrete defect-prone risk; missing tests alone are not a code defect; otherwise NO_FINDING.

## Do not flag
- Tests not supplied (unknown); trivial changes; risks covered by shown integration tests.

## Examples
Flag:
```python
def test_charge(): assert charge(order).status == "ok"      # no failure, retry or duplicate test for new retry logic
```
OK:
```python
def test_charge_retry_is_idempotent(): ... assert gateway.calls == 1
```

## Evaluation cases
1. New retry logic with only a happy-path test -> flag.
2. New authorization rule with allow and deny tests -> no finding.
3. New transactional write with rollback path untested -> flag.
4. Refactor without behavior change -> no finding.
