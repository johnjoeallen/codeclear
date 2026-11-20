# python/decimal-from-float: Decimal built from a float

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Values, state, and control flow |

## Intent
`Decimal(0.1)` carries the binary floating-point error, which defeats the purpose of using `Decimal` for money.

## Detect
A call to `Decimal(x)` (or `decimal.Decimal(x)`) where `x` is a float literal, a `float(...)` call, or an arithmetic expression involving a float literal.

## Do not flag
- `Decimal("0.1")`, `Decimal(int)`, `Decimal(str(x))`.
- `Decimal.from_float(x)`, which is explicit.

## Examples
Flag:
```python
price = Decimal(19.99)
```
OK:
```python
price = Decimal("19.99")
```

## Implementation notes
Python `ast`; variables of unknown type are not flagged. Equivalent: Ruff `RUF032`.

## Why deterministic
A `Decimal(...)` call with a float literal or float-typed argument is a syntactic fact.

Detected on tree-sitter `call` nodes named `Decimal` or `decimal.Decimal` with an argument that is, or contains, a float literal or a `float(...)` call. A variable is never flagged, because its type is unknown.

## Required tests
Float literal (finding), `float(x)` (finding), string literal (no finding), `from_float` (no finding).
