# typescript/truthiness-drops-valid-value: Truthiness check or default discards 0, "" or false

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | LOW |
| Detection type | hybrid |
| Source | typescript-review: Correctness; node-review: Values, serialization, and contracts |

## Intent
`if (!count)`, `x || default` and `x ? a : b` treat `0`, `""`, `false` and `NaN` as missing, so a valid value is replaced or rejected.

## Detect
- `!x`, `if (x)`, `x || literal`, `x ? ... : ...` where `x` has a type that includes `number`, `string` or `boolean` and the name suggests a count, amount, index, flag or offset (`count`, `total`, `amount`, `limit`, `offset`, `index`, `enabled`, `quantity`).
- Without type info, the same names only.

## Do not flag
- Operands typed as object, array or nullable object.
- `??`, `=== undefined`, `== null` checks.
- Cases where zero or empty is genuinely invalid and validated elsewhere.

## Examples
Flag:
```ts
const limit = req.query.limit || 20; // limit=0 is replaced
```
OK:
```ts
const limit = req.query.limit ?? 20;
```

## Implementation notes
Equivalent: `@typescript-eslint/strict-boolean-expressions`, `prefer-nullish-coalescing`. Anchor to the expression.

Detected, by name only (there is no type information), on `!x`, `if (x)`, `x || literal` and `x ? a : b` where `x` is an identifier or a property named like `count`, `total`, `amount`, `limit`, `offset`, `index`, `enabled`, `quantity`, `page`, `size`, `retries`, `port`, `age`, `price`, `balance`, `score`, `level`, `delay`, `timeout`, `min` or `max`. Names ending in `url`, `name`, `list`, `items`, `options` or `type` are skipped. `??` and explicit `=== undefined` / `== null` checks are never matched.

## Required tests
`||` default on numeric option (finding), `??` (no finding), object truthiness (no finding), `if (!count)` on number (finding).

## AI prompt
"Can this value legitimately be `0`, `''` or `false` for a reachable caller, and does the truthiness test then give a wrong result? Report only if you can name that input and the wrong outcome." Supply: the expression, the variable's type or source, and callers/validation.

## Why hybrid
Syntax finds the candidates, but whether zero or empty is valid is a domain question.
