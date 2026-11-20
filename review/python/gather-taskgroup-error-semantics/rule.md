# python/gather-taskgroup-error-semantics: gather/TaskGroup failure leaves siblings running or hides errors

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Asyncio and background work; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
`asyncio.gather` without `return_exceptions` raises the first error but does not cancel siblings, which keep running and may write; with `return_exceptions=True` errors become values that may be ignored; `TaskGroup` cancels siblings, which may abandon partial work.

## Detect
Candidates:
- `await asyncio.gather(...)` with side-effecting coroutines and no surrounding cleanup/cancel logic;
- `gather(..., return_exceptions=True)` whose result is not scanned for `BaseException` instances;
- `TaskGroup` bodies whose tasks perform non-idempotent writes.

## Do not flag
- Results iterated with `isinstance(r, BaseException)` handling.
- Read-only or idempotent tasks.

## Examples
Flag:
```python
await asyncio.gather(*(charge(o) for o in orders), return_exceptions=True)
```
OK:
```python
res = await asyncio.gather(*coros, return_exceptions=True)
errors = [r for r in res if isinstance(r, BaseException)]
if errors: raise ExceptionGroup("failed", errors)
```

## Implementation notes
Python `ast`; anchor to the `gather`/`TaskGroup` call. Account for the Python version.

## AI prompt
Question: "After one task fails or is cancelled, can sibling side effects continue or be abandoned so the overall result is inconsistent or reported as success?" Supply the tasks' bodies and the handling after the call. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The call shape is deterministic; harm depends on what the tasks do.
