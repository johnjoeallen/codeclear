# python/money-numeric-precision: Money rounding, mixed numeric types and lossy JSON conversion

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow |

## Intent
Monetary or precision-sensitive values mixed between `float`, `Decimal` and `int`, rounded inconsistently, or serialized through JSON/storage that cannot represent them, give wrong amounts. Construction from floats is the deterministic case, see `decimal-from-float`.

## Why AI
Which values are money, the required rounding mode and scale, and the limits of external stores or consumers (e.g. JavaScript numbers, DynamoDB) are domain and system context.

## AI prompt
"Does this code compute, round, compare or serialize a monetary/precision-sensitive value with a type or conversion that changes the amount?" Supply the calculation, rounding requirements, serializer (`json.dumps(default=float)`, ORM field types) and consumer format. Report only with a concrete trigger scenario (input amount, wrong result, changed line); otherwise NO_FINDING.

## Do not flag
- Non-monetary floats (metrics, ratios). Python ints exceeding 2**53 kept within Python.
- `Decimal` with explicit quantize and an agreed rounding mode.

## Examples
Flag:
```python
total = Decimal(price) * qty        # price is float; json.dumps(default=float) later
```
OK:
```python
total = (Decimal(str(price)) * qty).quantize(Decimal("0.01"), ROUND_HALF_UP)
```

## Evaluation cases
1. `round(amount * 1.2, 2)` on float prices -> flag.
2. `Decimal` amount serialized with `default=float` to a ledger service -> flag.
3. Integer cents added and stored in a BIGINT column -> no finding.
4. 64-bit id emitted as JSON number to a JS consumer shown in the MR -> flag.
