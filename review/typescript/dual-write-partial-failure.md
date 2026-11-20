# typescript/dual-write-partial-failure: Database plus broker/cache/search write without a recovery path

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects; node-bug-hunter: Trace the execution before judging it; Distributed workflows and microservices |

## Intent
Writing to a database and then a broker, cache, search index or another service is not atomic. A crash or failure between the steps leaves them inconsistent unless an outbox, retry or reconciliation covers the window. Outbox delivery itself can duplicate, so consumers still need idempotent effects.

## Why AI
Needs the ordered sequence of writes, which failure windows matter, and the visible recovery mechanism.

## AI prompt
"List the ordered writes. For a failure immediately before and after each, state the persisted state, and whether an outbox, retry, compensation or reconciliation shown recovers it. Check commit-then-crash, send-then-crash and concurrent retry. Report only with the window and its wrong end state; do not claim local transactions give atomicity, and do not require an outbox without a demonstrated need." Evidence: the code path, outbox/dispatcher, consumers, requirements.

## Do not flag
- Outbox written in the same transaction with a dispatcher shown.
- Best-effort side effects like cache warm-up.

## Examples
Flag:
```ts
await db.insert(order); await broker.publish('order.created', order); // crash between: no event
```
OK: insert order and outbox row in one transaction; dispatcher publishes.

## Evaluation cases
1. Insert then publish, no outbox/retry: flag.
2. Outbox row in the same transaction: ok.
3. Publish before commit, rollback possible: flag.
4. Cache set after DB write, cache has TTL and is a read-through: ok.
