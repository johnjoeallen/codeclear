# typescript/kafka-partition-key-ordering: Per-entity ordering not preserved

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
Kafka orders only within a partition. Without a stable entity key, or with per-partition concurrency that processes messages in parallel, updates for one entity can apply out of order.

## Why AI
Whether per-entity order is required is a domain rule, and keying/partition concurrency are split between producer and consumer.

## AI prompt
"Is per-entity ordering required? If so, is the message keyed by that entity, and does the consumer process a partition's messages serially (e.g. `partitionsConsumedConcurrently`, parallel `Promise.all` over a batch)? Name two messages that can be applied in the wrong order." Evidence: producer key, consumer concurrency config, ordering requirement.

## Do not flag
- Commutative events, or entities with versioned/last-write-wins guards.

## Examples
Flag:
```ts
await Promise.all(batch.messages.map(handle)); // same-entity events run concurrently
```
OK: group by key and process each group serially.

## Evaluation cases
1. Messages sent with no key and state transitions: flag.
2. Keyed by `orderId`, serial handler: ok.
3. Parallel batch handler on idempotent absolute-state events with version checks: ok.
