# typescript/rabbitmq-ack-paths: Ack/nack/reject not aligned with processing on every exit

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: RabbitMQ and background jobs; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
In manual-ack mode every exit path (success, throw, early return, channel closure, failed retry publish) must ack or nack after the durable commit. Missed exits leave deliveries unacked and exhaust prefetch; early acks lose work.

## Detect
A `channel.consume(queue, cb)` (no `noAck: true`) whose callback has an exit path with no `ack/nack/reject` (throw not caught, early `return`, branch without ack), or calls `ack` before awaiting the processing/DB write.

## Do not flag
- `noAck: true` where loss is accepted (documented).
- Callbacks with try/catch/finally that always settle the delivery.

## Examples
Flag:
```ts
ch.consume(q, async (m) => { await handle(m); ch.ack(m); }); // throw leaves it unacked
```
OK:
```ts
ch.consume(q, async (m) => { try { await handle(m); ch.ack(m); } catch { ch.nack(m, false, false); } });
```

## Implementation notes
Anchor to `consume`.

Detected on `channel.consume(queue, callback)` without `noAck: true`: a callback with no `ack`/`nack`/`reject` at all; a callback that awaits and has no `try` whose catch or finally settles the delivery; and one that calls `ack` before its first `await`. Early `return` branches without a settle are not followed separately.

## Required tests
Unsettled throw path (finding), try/catch settle (no finding), ack before await (finding), `noAck: true` (no finding).

## AI prompt
"List every exit of the callback. Which leave the delivery unsettled or acked before commit? Include retry-publish failure and channel closure. State the lost or stuck message." Evidence: consumer, prefetch config, retry/DLQ publish code.

## Why hybrid
Exit-path coverage is structural; channel closure and retry-publish semantics need context.
