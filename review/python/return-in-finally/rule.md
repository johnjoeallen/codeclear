# python/return-in-finally: return, break or continue inside finally

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Exceptions and cancellation |

## Intent
A `return`, `break` or `continue` in `finally` silently discards an exception propagating from the `try`.

## Detect
A `Return`, `Break` or `Continue` node whose nearest enclosing `try` construct places it in the `finalbody` (for `break` / `continue`, only if the loop is outside the `finally`).

## Do not flag
- A `return` inside a nested function or lambda defined in the `finally`.

## Examples
Flag:
```python
try:
    work()
finally:
    return result
```
OK:
```python
try:
    work()
finally:
    cleanup()
```

## Implementation notes
Python `ast`. Equivalent: Ruff `B012`, pylint `lost-exception`.

## Why deterministic
A `return`/`raise`/`break` located in a `finally` body is a syntactic fact.

The nearest enclosing `finally_clause`, function, lambda, class or loop decides: a `return` is flagged when the finally is nearest, and a `break` or `continue` only when no loop sits between it and the finally.

## Required tests
Return, break and continue in `finally` (findings); nested function return (no finding).
