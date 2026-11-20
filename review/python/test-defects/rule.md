# python/test-defects: Unawaited async tests, leaking fixtures, assertions that cannot fail and nondeterminism

| Field | Value |
|---|---|
| Applies to | `**/test_*.py`, `**/*_test.py`, `**/conftest.py` |
| Category | TESTING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Test evidence |

## Intent
Tests that cannot fail or are flaky give false confidence: async tests without a runner marker or with unawaited calls/assertions, assertions that are always true, mocks bypassing the code under test, fixtures leaking state, and dependence on time, randomness, network or ordering.

## Detect
Candidates:
- `async def test_*` without `@pytest.mark.asyncio`/anyio marker and no `asyncio_mode = auto` shown; calls to coroutine functions in a test without `await`;
- `assert (cond, "msg")`, `assert True`, `assert x is not None` immediately after assigning a non-None literal; `assertTrue(x, y)` misuse; `pytest.raises` block containing only the setup;
- `time.sleep`, `datetime.now()`, `random.*` (unseeded), real `requests.*`/`socket` calls in unit tests;
- `@patch` targeting the function under test itself; module-level mutable fixtures without teardown.

## Do not flag
- `asyncio_mode = auto` configuration; frozen time/seeded random; integration tests marked as such.

## Examples
Flag:
```python
async def test_save():
    repo.save(item)                # coroutine never awaited
    assert (item.id, "saved")      # tuple always truthy
```
OK:
```python
@pytest.mark.asyncio
async def test_save():
    await repo.save(item); assert item.id
```

## Implementation notes
Python `ast` over test files; equivalent pytest `-W error::RuntimeWarning`, Ruff `PT`/`F631`. Anchor to the assertion/call.

For test files. Flags an `async def test_*` with no asyncio, anyio or trio decorator in a module that does not mention `pytestmark` or `asyncio_mode`; `assert (cond, "msg")`; `assert True`; `assertTrue(a, b)` with a non-string `b`; and `time.sleep`, `datetime.now`, unseeded `random.*`, `requests.*` and `socket` calls in a module that mentions none of `freeze_time`, `seed`, `responses`, `mock`, `patch` or `monkeypatch`. Files under `integration`, `e2e` or `functional` are skipped. `@patch` of the function under test and `pytest.raises` blocks with only setup are not detected.

## AI prompt
Question: "Can this test pass while the behavior it names is broken (never runs the path, asserts nothing meaningful, mocks the unit under test)?" Supply the test, the code under test and test configuration. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Patterns are syntactic; whether the test is vacuous depends on the code under test.
