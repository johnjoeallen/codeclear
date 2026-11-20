# python/none-vs-falsy-confusion: None conflated with zero, false or empty; unchecked missing values

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow |

## Intent
Code treats `None`, `0`, `False` and `""` alike (`if not x`, `x or default`), or dereferences missing keys, nullable query results and unknown enum values at an external boundary, producing a wrong result or crash.

## Why AI
Whether `0`/empty is a valid value, whether a key can be absent, and whether a query can return no row depends on the domain and data source, not syntax.

## AI prompt
"Does a changed truthiness test, `or` default, `dict[key]`, `.get()` result, `.first()`/`scalar()` result, or enum conversion mis-handle a value that is valid or absent at this boundary?" Supply the changed function, the source of the value (API schema, model column nullability, enum definition) and callers. Report only with a concrete trigger scenario (input value, wrong outcome, changed line); otherwise NO_FINDING.

## Do not flag
- `is None` checks and deliberate sentinels.
- Fields documented as always positive/non-empty or validated earlier on the shown path.

## Examples
Flag:
```python
timeout = cfg.get("timeout") or 30   # timeout=0 means "no timeout"
```
OK:
```python
timeout = cfg.get("timeout")
if timeout is None: timeout = 30
```

## Evaluation cases
1. `qty = payload.get("qty") or 1` where schema allows `0` -> flag.
2. `user = session.query(User).first(); user.email` with no None check on a path where ids come from the request -> flag.
3. `name = data.get("name") or "anonymous"` where empty name is documented as invalid -> no finding.
4. `Status(value)` on a broker payload with an enum that has no fallback, inside a try that dead-letters -> no finding.
