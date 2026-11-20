# typescript/remote-call-in-transaction: Remote call, sleep or broker send inside a DB transaction

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions |

## Intent
Remote calls inside a transaction hold row locks and a pooled connection for the call's duration (lock contention, pool exhaustion), and their side effects (email, payment, publish) cannot be rolled back.

## Detect
Inside a transaction callback or between `BEGIN` and `COMMIT`: `fetch`, `axios.*`, `http(s).request`, gRPC client calls, `producer.send`, `channel.publish`, `sqs.send`, `sns.publish`, `sendMail`, `setTimeout`-based sleeps.

## Do not flag
- Calls to the same database.
- Calls explicitly in an `afterCommit`/outbox write.
- Fast in-process calls.

## Examples
Flag:
```ts
await prisma.$transaction(async (tx) => { await tx.order.create(d); await fetch(paymentUrl, ...); });
```
OK:
```ts
const o = await prisma.$transaction((tx) => tx.order.create(d)); await fetch(paymentUrl, ...);
```

## Implementation notes
Anchor to the remote call.

## Required tests
`fetch` in callback (finding), after transaction (no finding), `producer.send` in callback (finding), DB-only (no finding).

## AI prompt
"Can this remote call hold locks or the pool for long, or have a side effect that rollback cannot undo? Name the concrete lock duration, pool exhaustion or irreversible effect." Evidence: transaction body, callee timeout, pool size.

## Why hybrid
Calls inside transaction scope are syntactic; impact depends on latency, pool and reversibility.
