# python/raise-without-cause: New exception raised in handler without chaining

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | python-review: Exceptions and cancellation |

## Intent
Raising a new exception inside `except` loses an explicit cause. Python sets an implicit context, but `raise ... from e` states the relationship, and `from None` hides it.

## Detect
- `raise NewError(...)` inside an `except` block, without `from`.
- `raise NewError(...) from None` where the message does not include information from the original exception.

## Do not flag
- Bare `raise`.
- `raise ... from e` and `raise ... from err.__cause__`.
- `from None` with a comment explaining why the cause is hidden.

## Examples
Flag:
```python
except KeyError:
    raise ConfigError("missing key")
```
OK:
```python
except KeyError as e:
    raise ConfigError("missing key") from e
```

## Implementation notes
Python `ast`. Equivalent: Ruff `B904`, pylint `raise-missing-from`.

## Why deterministic
A `raise` inside an `except` body with and without a `from` clause is a syntactic fact.

## Required tests
No `from` (finding), `from e` (no finding), bare `raise` (no finding), `from None` with no info (finding).
