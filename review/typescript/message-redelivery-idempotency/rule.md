# typescript/message-redelivery-idempotency: Non-idempotent side effects under at-least-once delivery

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka; node-review: RabbitMQ and background jobs; node-bug-hunter: Distributed workflows and microservices |

## Intent
Kafka, RabbitMQ, SQS and job queues can redeliver. Consumers with non-idempotent effects (charge, email, counter increment) duplicate them. Producer idempotence does not deduplicate application database writes.

## Why AI
Needs the effect's idempotency, any dedup key or unique constraint, and the delivery configuration.

## AI prompt
"Assume the same message is delivered twice (and processed concurrently). Does the second run repeat a non-idempotent effect? Check message-id dedup, unique constraints and conditional writes already present. Report only with the duplicate effect." Evidence: consumer, effect code, dedup storage, delivery config.

## Do not flag
- Natural idempotency (upsert by key, set-to-value), or dedup guarded by atomic key/constraint.

## Examples
Flag:
```ts
await db.query('UPDATE acct SET bal = bal + $1', [m.amount]); // replay doubles credit
```
OK: insert the `message_id` in a unique table in the same transaction first.

## Evaluation cases
1. Increment with no dedup: flag.
2. Upsert by `orderId`: ok.
3. Dedup by `INSERT` unique `messageId` in the same transaction: ok.
