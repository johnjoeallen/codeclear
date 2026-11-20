# java/rabbit-ack-nack-handling: Ack/nack/reject behaviour wrong on success or failure

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: RabbitMQ |

## Intent
Incorrect acknowledgement loses messages (ack before processing, ack on failure) or leaves deliveries unacked so a prefetch-limited consumer stalls.

## Why AI
Behaviour depends on the container's acknowledge mode, exception translation and prefetch, which come from configuration.

## AI prompt
"From the visible acknowledge mode and listener code, trace the success and each failure path: is the message acked, nacked/rejected, or left unacked? Report only a path that loses a message or leaves an unacked delivery blocking a prefetch-limited consumer, with the config evidence." Evidence: `@RabbitListener` method, container factory, `Channel` usage, prefetch setting.

## Do not flag
- AUTO ack mode with exceptions propagating to the container.
- Manual ack with `basicAck` in success and `basicNack` in a `finally` or catch.

## Examples
Flag: manual ack mode, listener throws before `basicAck` and has no catch, prefetch 1.
OK: `try { process(m); ch.basicAck(tag, false); } catch (Exception e) { ch.basicNack(tag, false, false); }`.

## Evaluation cases
1. Manual mode, exception path never acks/nacks - report.
2. AUTO mode, exception propagates - no report.
3. `basicAck` before processing - report.
4. Mode not shown, container default - no report.
