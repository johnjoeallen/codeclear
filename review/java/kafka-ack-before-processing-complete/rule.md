# java/kafka-ack-before-processing-complete: Offset commit/ack timing vs processing and DB commit

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Kafka |

## Intent
Acknowledging or committing an offset before the record's effects are durable loses the message on a crash; acknowledging long after the DB commit duplicates the side effect on redelivery.

## Why AI
The outcome depends on configured ack mode, error handler, transaction manager and which statements run before the ack, spread across listener code and configuration. Listener code alone must not be used to infer loss or retry behaviour.

## AI prompt
"Using the visible `AckMode`, `enable.auto.commit`, error handler and the listener body, state when the offset is committed relative to processing and the DB commit. Report only if a crash or exception at a named point loses or reprocesses the record, with the configuration evidence. If configuration is not shown, say what would confirm it and do not report." Evidence: `@KafkaListener` method, container factory config, consumer properties, transaction setup.

## Do not flag
- Container-managed ack with the default error handler where processing exceptions propagate.
- Idempotent processing where redelivery is harmless (see `kafka-non-idempotent-consumer`).

## Examples
Flag: manual `ack.acknowledge()` first line of the listener, then a DB write that may fail.
OK: `acknowledge()` after the successful commit, exceptions propagate to the error handler.

## Evaluation cases
1. `acknowledge()` before DB save, MANUAL ack mode shown - report.
2. Container RECORD mode, exceptions propagate - no report.
3. `enable.auto.commit=true` with slow processing shown - report.
4. Config not shown, listener swallows nothing - no report.
