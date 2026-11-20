# typescript/missing-return-after-response: Response or next(err) sent without returning

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
After `res.send/json/status().end` or `next(err)` inside a guard branch, execution continues, so the handler may send a second response (`ERR_HTTP_HEADERS_SENT`) or perform the operation the guard meant to prevent.

## Detect
In an Express/Connect-style handler, an `if` branch containing `res.send|json|sendStatus|end|redirect(...)` or `next(err)` that is not preceded by `return` / `throw` and is followed by further statements in the enclosing function that call `res.*` or perform I/O.

## Do not flag
- Branches ending in `return`, or if/else where the continuation is in the else.
- Final statement of the handler.

## Examples
Flag:
```ts
if (!user) res.status(404).json({ error: 'nf' });
res.json(user);
```
OK:
```ts
if (!user) return res.status(404).json({ error: 'nf' });
res.json(user);
```

## Implementation notes
Anchor to the unreturned response call. Equivalent: `consistent-return`, `callback-return`.

## Required tests
Unreturned 404 then send (finding), returned (no finding), else branch (no finding), last statement (no finding).

## Why deterministic
Reachability of a subsequent response call after an unreturned response is control flow within one function.
