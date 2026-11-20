# typescript/timestamp-unit-timezone-mismatch: Timestamp units, invalid dates or timezone conversion

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Values, serialization, and contracts |

## Intent
Mixing seconds and milliseconds, using unvalidated `new Date(x)` (Invalid Date), or converting between zones across services yields wrong expiry, ordering or day boundaries.

## Why AI
Units and zones are defined by other services' contracts and column types; a wall-clock date is valid when intentional.

## AI prompt
"Do the units (s vs ms), time zone and validity of this date value match the producer and consumer contracts? Give the concrete input and wrong result." Evidence: both sides of the contract (schema, JWT `exp`, DB column type, API doc), the code.

## Do not flag
- Local wall-clock dates that are intentional (birthdays, calendar days).
- Values only passed through unchanged.

## Examples
Flag:
```ts
if (token.exp < Date.now()) expired(); // exp is in seconds
```
OK:
```ts
if (token.exp * 1000 < Date.now()) expired();
```

## Evaluation cases
1. JWT `exp` compared to `Date.now()`: flag.
2. `new Date(req.body.date)` used for storage with no `isNaN` check: flag.
3. `setTimeout(fn, ttlSeconds)` where ttl is documented seconds: flag.
4. Birthday stored as local date string: ok.
