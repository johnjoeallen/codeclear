# typescript/transaction-client-not-used: Query escapes the transaction-bound client

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions; node-bug-hunter: Database invariants and rollback |

## Intent
Inside a transaction callback every query must use the transaction's client/session. A global client, repository injected outside, or unawaited call runs on another connection and is not rolled back.

## Detect
Inside `prisma.$transaction(async (tx) => ...)`, `sequelize.transaction(...)`, `knex.transaction(trx => ...)`, `dataSource.transaction(m => ...)`, `session.withTransaction(...)`: a call on the outer `prisma`, `db`, `knex`, `Model.find*`, `repo.*` rather than `tx`/`trx`/`manager`/`{ session }`; or a query promise not awaited or returned.

## Do not flag
- Intentional separate-connection work with a comment (audit log) shown.
- Helpers that take and use the transaction handle.

## Examples
Flag:
```ts
await prisma.$transaction(async (tx) => { await tx.order.create(d); await prisma.stock.update(u); });
```
OK:
```ts
await prisma.$transaction(async (tx) => { await tx.order.create(d); await tx.stock.update(u); });
```

## Implementation notes
Anchor to the escaping query. Helpers need cross-file resolution.

Detected on a call made on `prisma`, `db`, `knex`, `dataSource`, `sequelize`, `connection`, `pool`, `em`, a `*Repo`/`*Repository`, or a capitalised model name, inside the callback of a call named `transaction`, `$transaction`, `withTransaction` or `runInTransaction`, when no argument mentions `transaction`, `session`, `trx`, `tx`, `manager` or `t`. A helper that is passed the handle is not seen as outer.

## Required tests
Outer client in callback (finding), all `tx` (no finding), unawaited `tx` call (finding), helper receiving `tx` (no finding).

## AI prompt
"Does this query run on the transaction's connection? Trace repository helpers actually shown. Name the write that would persist after rollback." Evidence: callback, helpers, ORM and version.

## Why hybrid
Local escapes are syntactic; helper and ORM context-binding (CLS, ALS) can make them valid.
