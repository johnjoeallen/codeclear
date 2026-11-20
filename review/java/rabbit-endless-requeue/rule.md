# java/rabbit-endless-requeue: Permanent failure requeued forever

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: RabbitMQ |

## Intent
A message that always fails and is requeued (`defaultRequeueRejected` true, `basicNack(..., requeue=true)`) loops indefinitely, burning CPU and blocking the queue unless retries or a dead-letter exchange bound it.

## Why AI
Whether the loop is bounded depends on retry interceptor, `x-dead-letter-exchange`, delivery-limit and queue definitions that may sit outside the changed file; a missing DLX must not be assumed.

## AI prompt
"Can a permanent failure (e.g. invalid payload) in this listener cause the message to be requeued indefinitely? Check visible retry settings (`spring.rabbitmq.listener...retry`, `AmqpRejectAndDontRequeueException`), DLX arguments and quorum `delivery-limit`. Report only if no bound is shown and queue config is in context, naming the failing input." Evidence: listener, container/retry config, queue declaration.

## Do not flag
- Listeners throwing `AmqpRejectAndDontRequeueException` or with bounded retry + DLX.
- Queue configuration not supplied.

## Examples
Flag: `basicNack(tag, false, true)` for any exception, queue declared without DLX or delivery limit.
OK: `defaultRequeueRejected=false` with a DLX bound queue.

## Evaluation cases
1. Always-requeue nack, queue declared without DLX - report.
2. Bounded retry (3) then reject to DLX - no report.
3. Requeue with quorum queue `x-delivery-limit` set - no report.
4. Queue definition absent - no report.
