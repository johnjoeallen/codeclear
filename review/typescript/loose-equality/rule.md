# typescript/loose-equality: == or != instead of === or !==

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | typescript-review: Correctness |

## Intent
Loose equality applies type coercion, so `0 == ""` and `"1" == 1` are true.

## Detect
A `BinaryExpression` using `==` or `!=`.

## Do not flag
- Comparison with `null` (`x == null`), which intentionally covers `undefined`.
- Both operands have the same statically known primitive type.

## Examples
Flag:
```ts
if (user.id == req.params.id) {}
```
OK:
```ts
if (user.id === Number(req.params.id)) {}
if (value == null) {}
```

## Implementation notes
Equivalent: ESLint `eqeqeq` with `{"null": "ignore"}`. Anchor to the operator line.

Detected on `binary_expression` nodes with `==` or `!=`. A comparison with `null`, two literals of the same primitive type, and `typeof x == "string"` are skipped; a variable whose type is known to be primitive is not recognised, since there is no type checker.

## Required tests
`==` between variables (finding), `===` (no finding), `== null` (no finding), `!=` (finding).
