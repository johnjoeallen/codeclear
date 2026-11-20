# python/context-manager-exit-suppression: __exit__/__aexit__ suppresses or replaces the original failure

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Exceptions and cancellation; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
A context manager whose `__exit__`/`__aexit__` returns a truthy value (or uses `contextlib.suppress`/`@contextmanager` that swallows) hides exceptions, so a failed block looks successful (e.g. a transaction commits). Cleanup that raises can replace the original error. `return-in-finally` covers `finally`.

## Detect
Candidates:
- `__exit__`/`__aexit__` with `return True` or returning a non-constant expression;
- `@contextmanager` generators whose `try: yield` has an `except` that does not re-raise;
- `contextlib.suppress(Exception|BaseException)` around writes/commits;
- cleanup calls inside `__exit__` not guarded so they can mask the in-flight exception.

## Do not flag
- Suppression of a narrow expected exception (`suppress(FileNotFoundError)`) around idempotent cleanup.

## Examples
Flag:
```python
def __exit__(self, et, ev, tb):
    self.conn.commit()
    return True
```
OK:
```python
def __exit__(self, et, ev, tb):
    (self.conn.rollback if et else self.conn.commit)()
    return False
```

## Implementation notes
Python `ast`; anchor to the `return`/`except`/`suppress`.

Detected on `__exit__`/`__aexit__` with a `return` of a true or non-constant value, or with an unguarded `close`/`release`/`rollback`/`commit` call; on a `@contextmanager` function with a `try` around a `yield` and an `except` that has no `raise`; and on `with suppress(Exception)` or `suppress(BaseException)`. A narrow `suppress(FileNotFoundError)` is not matched.

## AI prompt
Question: "Does the suppressed or replaced exception represent a failure the caller must observe (rollback, error response, retry)?" Supply the context manager and its call sites. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Suppression is syntactic; whether it is harmful depends on what the wrapped block does.
