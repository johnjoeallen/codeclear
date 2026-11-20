# typescript/kafka-poison-message-handling: Swallowed failures, poison loops and skipped records

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
A consumer that rethrows forever on a permanently bad record blocks its partition; one that catches and continues silently drops it. Dead-letter publishing can fail too.

## Why AI
Whether a failure is permanent, bounded retry exists and a DLQ is configured needs consumer config and topic setup.

## AI prompt
"For a record that always fails, what happens: infinite redelivery, silent skip, or bounded retry then dead-letter? If dead-letter publishing fails, is the record lost? Report only with the concrete bad record and outcome; do not assert missing routing from a consumer file alone." Evidence: consumer code, retry/DLQ config, topic declarations.

## Do not flag
- Bounded retry with a visible DLQ whose publish failure is handled.

## Examples
Flag:
```ts
try { await handle(m); } catch (e) { log.error(e); } // failure is skipped, offset advances
```
OK: retry N times then publish to `topic.dlq`, rethrow if the DLQ publish fails.

## Evaluation cases
1. Catch-and-log in `eachMessage`: flag.
2. Unbounded retry on JSON parse error: flag.
3. Retry with DLQ and DLQ failure rethrown: ok.
