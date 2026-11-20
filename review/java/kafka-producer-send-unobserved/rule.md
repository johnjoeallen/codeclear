# java/kafka-producer-send-unobserved: Producer send result ignored when success matters

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Kafka |

## Intent
`KafkaTemplate.send`/`producer.send` is asynchronous; ignoring its future or callback hides failed sends. Durability also depends on `acks`, idempotence and retries.

## Detect
A call to `kafkaTemplate.send(...)`, `producer.send(record)` or `sendDefault` used as an expression statement (result discarded) with no callback argument, no `.get()`, no `whenComplete/addCallback`.

## Do not flag
- Results returned, stored or chained with a failure handler.
- Producer callbacks that log and record failures.
- Explicit fire-and-forget telemetry.

## Examples
Flag:
```java
kafkaTemplate.send("payments", id, event);
```
OK:
```java
kafkaTemplate.send("payments", id, event).whenComplete((r, ex) -> { if (ex != null) failures.record(id, ex); });
```

## Implementation notes
AST. Anchor to the send call. Do not prescribe producer settings without a stated guarantee.

## Required tests
Discarded send (candidate), `.get()` (none), `whenComplete` (none), callback overload (none).

## Why hybrid
Whether send success matters, and what `acks`/idempotence provide, depends on the guarantee required.

## AI prompt
"Does the caller need delivery of this record to succeed? If so, can a failed send be lost with the visible `acks`/retries/idempotence settings? Report only with the failure scenario." Evidence: the send, producer config, calling flow and its stated guarantee.
