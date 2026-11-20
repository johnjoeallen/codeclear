# typescript/float-money-arithmetic: Money computed with binary floating point

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Values, serialization, and contracts |

## Intent
Arithmetic on `Number` amounts gives errors such as `0.1 + 0.2`, and ad hoc rounding drifts totals.

## Detect
`+ - * /`, `toFixed`, `Math.round` or `parseFloat` applied to identifiers named `price`, `amount`, `total`, `balance`, `fee`, `tax`, `cost`, `vat` (or `*Amount`, `*Price`) with no decimal library (`decimal.js`, `big.js`, `dinero`) or integer-minor-unit convention in the file.

## Do not flag
- Integer minor units (`cents`, `pence`) with integer arithmetic.
- Display-only formatting (`Intl.NumberFormat`).
- Non-monetary quantities.

## Examples
Flag:
```ts
const total = items.reduce((s, i) => s + i.price * i.qty, 0);
```
OK:
```ts
const total = items.reduce((s, i) => s.plus(new Decimal(i.price).times(i.qty)), new Decimal(0));
```

## Implementation notes
Anchor to the arithmetic expression.

## Required tests
Float `price * qty` (finding), Decimal (no finding), `cents + cents` (no finding), `Intl.NumberFormat` (no finding).

## AI prompt
"Is this value real money used in a stored or transmitted total, and do rounding or float errors change the outcome?" Evidence: variable origin/units, where the result is used.

## Why hybrid
Names suggest money but cannot prove it; units and use decide.
