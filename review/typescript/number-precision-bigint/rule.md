# typescript/number-precision-bigint: Large identifiers or counters in Number, BigInt in JSON

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Values, serialization, and contracts |

## Intent
`Number` loses precision above 2^53, and `JSON.stringify` throws on `BigInt`. Database bigint ids and large counters get corrupted or crash serialization.

## Detect
- `Number(x)`, `parseInt(x)`, `parseFloat(x)` or unary `+x` where `x` is named `*Id`, `id`, `*Count`, `balance` and comes from a DB row, driver or JSON field of a bigint column.
- `JSON.stringify(v)` or `res.json(v)` where `v` contains a `BigInt(...)` / `123n` value and no `replacer` or `toJSON`.

## Do not flag
- Ids known to be UUID or string.
- A `BigInt.prototype.toJSON` override or replacer in scope.

## Examples
Flag:
```ts
res.json({ id: BigInt(row.id) });
```
OK:
```ts
res.json({ id: row.id.toString() });
```

## Implementation notes
Anchor to the conversion. Type info helps; fall back to names.

Detected on `Number(x)`, `parseInt(x)`, `parseFloat(x)` and unary `+x` where `x` is a property named like an id, count, balance or total of `row`, `rows[n]`, `result`, `record`, `entity`, `data` or `item`; and on `JSON.stringify(v)` or `res.json(v)` where the argument contains `BigInt(` or a `123n` literal, with no second argument. A file that mentions `uuid`, `BigInt.prototype.toJSON` or `replacer` is skipped.

## Required tests
BigInt in `res.json` (finding), `toString()` (no finding), `Number(row.id)` on bigint column (finding), UUID (no finding).

## AI prompt
"Can the value exceed 2^53 or is a BigInt serialized as JSON here? Check the column type or wire contract supplied." Evidence: schema/column type, consumer contract, the code.

## Why hybrid
Syntax finds the conversions; the value range and wire contract decide whether it breaks.
