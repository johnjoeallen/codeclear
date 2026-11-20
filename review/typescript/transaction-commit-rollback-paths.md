# typescript/transaction-commit-rollback-paths: Transaction commit, rollback or release missed on some path

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions; node-bug-hunter: Database invariants and rollback |

## Intent
Manual transactions must commit, roll back and release the connection on every path. A catch that swallows the error inside a transaction callback can commit partial writes.

## Detect
- `BEGIN`/`beginTransaction`/`startTransaction`/`pool.connect()` without `release()`/`end()` in a `finally`, or with `ROLLBACK` missing in the catch.
- Inside an ORM transaction callback, a `try/catch` that does not rethrow, or returns a success-shaped value in `catch`.
- `commit()` not awaited.

## Do not flag
- ORM managed callbacks that rethrow.
- Catches that rethrow after cleanup.

## Examples
Flag:
```ts
await db.transaction(async (t) => { try { await a(t); await b(t); } catch (e) { log(e); } });
```
OK:
```ts
const c = await pool.connect(); try { await c.query('BEGIN'); ...; await c.query('COMMIT'); } catch (e) { await c.query('ROLLBACK'); throw e; } finally { c.release(); }
```

## Implementation notes
Anchor to the `BEGIN`/transaction start. Also covers failed commit paths.

## Required tests
No release in finally (finding), swallowed catch in callback (finding), full try/catch/finally (no finding), rethrow (no finding).

## AI prompt
"On each failure path, is the transaction rolled back, the connection released and the failure visible to the caller? Name the partial write that is committed." Evidence: transaction code, ORM semantics.

## Why hybrid
Missing pieces are syntactic; whether the swallowed error leaves partial state needs the domain.
