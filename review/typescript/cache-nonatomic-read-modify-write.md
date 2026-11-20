# typescript/cache-nonatomic-read-modify-write: Non-atomic cache updates or separate value and expiry

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Caches and distributed locks |

## Intent
`GET` then `SET`, or `SET` followed by a separate `EXPIRE`, is not atomic. Concurrent access loses updates, or a crash between commands leaves a key without expiry.

## Why AI
Safety depends on transactions, Lua scripts, atomic commands and the invariant, which must be checked together.

## AI prompt
"Is there a read-modify-write or multi-command sequence on a key where concurrent access or failure between commands violates a shown invariant (counter, rate limit, lock)? Account for `MULTI`, scripts, `INCR`, `SET ... EX NX` already used. Give the interleaving." Evidence: command sequence, atomic primitives available, invariant.

## Do not flag
- `INCR`, `SET key v EX n`, `MULTI/EXEC`, Lua scripts; best-effort caches.

## Examples
Flag:
```ts
const n = Number(await redis.get(k)); await redis.set(k, n + 1); await redis.expire(k, 60);
```
OK: `await redis.multi().incr(k).expire(k, 60).exec();`

## Evaluation cases
1. get/set counter used as a rate limit: flag.
2. `INCR` plus `EXPIRE` in `MULTI`: ok.
3. `SET` then `EXPIRE` for a lock key: flag.
