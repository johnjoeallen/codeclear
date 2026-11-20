# python/bare-except: Bare except or BaseException catch

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Exceptions and cancellation |

## Intent
`except:` and `except BaseException:` also catch `KeyboardInterrupt`, `SystemExit` and, on older Python, `asyncio.CancelledError`, which blocks shutdown and cancellation.

## Detect
An `ExceptHandler` with no type, or with type `BaseException`, whose body does not end in a bare `raise`.

## Do not flag
- Handlers that re-raise after cleanup.
- `except Exception:` (a separate broad-catch concern).

## Examples
Flag:
```python
try:
    run()
except:
    log.error("failed")
```
OK:
```python
try:
    run()
except ValueError:
    log.error("failed")
```

## Implementation notes
Python `ast`. Equivalent: Ruff `E722`, `BLE001`.

## Why deterministic
The handler's exception type and a trailing bare `raise` are syntactic facts.

Detected on the tree-sitter tree: an `except_clause` whose only named child is its block, or whose type is `BaseException`. A handler is skipped when the last statement of its block is a bare `raise`.

## Required tests
Bare except (finding), `BaseException` (finding), bare except with `raise` (no finding), `except ValueError` (no finding).
