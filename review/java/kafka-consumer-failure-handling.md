# java/kafka-consumer-failure-handling: Swallowed errors, poison messages and batch failures

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Kafka |

## Intent
A Kafka listener can swallow an exception (record skipped), retry a poison message forever, or acknowledge a batch containing failed records.

## Why AI
Outcomes depend on the container's error handler, retry/DLT configuration and batch semantics, which must be read from configuration rather than inferred from the listener.

## AI prompt
"For each failure path in this listener (exception, bad payload, partial batch failure), state from the visible container configuration whether the record is retried, dead-lettered, skipped or acknowledged. Report only a path where a record is lost, endlessly retried, or acked despite failure, naming the config evidence. Do not infer behaviour without it." Evidence: listener, `DefaultErrorHandler`/`DeadLetterPublishingRecoverer` config, ack mode, batch flag.

## Do not flag
- Catch blocks that publish to a DLT or rethrow to the configured error handler.
- Failures where the configured handler is shown to bound and route them.

## Examples
Flag: `catch (Exception e) { log.error("bad", e); }` in a `@KafkaListener`, offset then committed.
OK: exception rethrown with `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` and bounded backoff.

## Evaluation cases
1. Listener catches and logs, record auto-acked - report.
2. Batch listener acks whole batch after 3rd record fails and is caught - report.
3. Rethrow with DLT configured - no report.
4. Exception thrown, error handler config not shown - no report.
