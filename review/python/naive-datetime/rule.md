# python/naive-datetime: Naive datetime creation

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | python-review: Values, state, and control flow |

## Intent
Naive datetimes carry no timezone. Mixing them with aware values raises `TypeError`, and they are misread when serialised across services.

## Detect
Calls to `datetime.now()` or `datetime.utcnow()` with no `tz`, `datetime.utcfromtimestamp(...)`, and `datetime.fromtimestamp(x)` with no `tz`.

## Do not flag
- Calls with a `tz=` argument or `datetime.now(timezone.utc)`.
- Local-only scripts and test files.

## Examples
Flag:
```python
created = datetime.utcnow()
```
OK:
```python
created = datetime.now(timezone.utc)
```

## Implementation notes
Python `ast`. Equivalent: Ruff `DTZ001`-`DTZ006`. `utcnow` is deprecated since Python 3.12.

## Why deterministic
A `datetime` constructor call without a timezone argument is a syntactic fact.

Detected on `datetime.<method>` and `datetime.datetime.<method>` calls. `now()` and `fromtimestamp(x)` are flagged without `tz=`/`tzinfo=` or a second positional argument; `utcnow()` and `utcfromtimestamp()` always. A `from datetime import now` style call is not seen.

## Required tests
`now()` (finding), `utcnow()` (finding), `now(timezone.utc)` (no finding), `fromtimestamp(x, tz=UTC)` (no finding).
