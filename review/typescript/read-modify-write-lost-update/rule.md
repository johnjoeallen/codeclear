# typescript/read-modify-write-lost-update: Read-modify-write not protected against concurrent updates

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Databases, ORM, and transactions; node-bug-hunter: Database invariants and rollback |

## Intent
A select then update of a counter, balance or status, even in a transaction, loses updates unless an atomic update, row lock, version column, serializable isolation or constraint protects it.

## Why AI
Safety depends on isolation level, locking hints, constraints and the invariant, which live in DB config and schema.

## AI prompt
"Name the invariant. Construct two concurrent requests that interleave the read and the write and end in a state violating it. Check visible isolation, `FOR UPDATE`, version/`WHERE` guards, atomic `SET x = x + 1` and unique constraints first. Report only with the interleaving." Evidence: queries, transaction settings, schema/constraints.

## Do not flag
- Atomic increments, conditional updates checking affected rows, optimistic version checks, `SELECT ... FOR UPDATE`.

## Examples
Flag:
```ts
const a = await Account.findByPk(id); a.balance -= amt; await a.save();
```
OK:
```ts
await Account.update({ balance: literal(`balance - ${amt}`) }, { where: { id, balance: { [Op.gte]: amt } } });
```

## Evaluation cases
1. Read balance, validate, write new balance under default isolation: flag.
2. `UPDATE ... SET n = n + 1`: ok.
3. Version column with `where version = ?` and affected-row check: ok.
