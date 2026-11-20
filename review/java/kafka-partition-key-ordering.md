# java/kafka-partition-key-ordering: Per-entity ordering without a stable partition key

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Kafka |

## Intent
Kafka orders records only within a partition. If consumer logic assumes events for one entity arrive in order but producers use no key (or a different key), updates can be processed out of order.

## Why AI
Both producer key choice and consumer assumptions must be understood, and these are often in different services or files.

## AI prompt
"Does the consumer logic depend on per-entity ordering (state transitions, last-write-wins, create-before-update)? If so, find the producer's partition key in the supplied code. Report only if the key is absent or not the entity id and name a reordering that gives a wrong result. For batch listeners, also state what happens when a record in the middle of the batch fails." Evidence: consumer code, producer send code, topic partition config.

## Do not flag
- Consumers that are order-insensitive (commutative updates, version checks that discard stale events).
- Producers shown to key by the entity id.

## Examples
Flag: consumer applies `STATUS_CHANGED` events sequentially; producer calls `send(topic, event)` with no key.
OK: `send(topic, event.orderId(), event)` and the consumer ignores events with older versions.

## Evaluation cases
1. Producer sends without key, consumer assumes order - report.
2. Key = entity id - no report.
3. Consumer drops events with older version - no report.
4. Producer code not shown - no report.
