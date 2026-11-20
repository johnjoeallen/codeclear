# python/is-literal-comparison: Identity comparison with a literal

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Values, state, and control flow |

## Intent
`is` tests identity, not equality. Comparing against a string, number or tuple literal depends on interpreter caching.

## Detect
A `Compare` node using `is` or `is not` where either operand is a constant `str`, `bytes`, `int`, `float` or a tuple literal.

## Do not flag
- `is None`, `is not None`, `is True`, `is False`, `is Ellipsis`.
- Comparisons with an intentional sentinel object created in the module.

## Examples
Flag:
```python
if status is "ready":
    pass
```
OK:
```python
if status == "ready":
    pass
```

## Implementation notes
Python `ast`. CPython emits `SyntaxWarning` for this too. Equivalent: Ruff `F632`.

## Why deterministic
An `is`/`is not` comparison against a literal is a syntactic fact.

Detected on `comparison_operator` nodes that use `is` or `is not`. Operands of type string, number, tuple or concatenated string count as literals; `None`, `True`, `False` and `...` are their own node types and do not. A sentinel object created in the module is a name, not a literal, so it is not flagged.

## Required tests
`is "x"` (finding), `is 0` (finding), `is None` (no finding), `is True` (no finding).
