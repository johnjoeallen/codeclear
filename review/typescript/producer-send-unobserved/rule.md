# typescript/producer-send-unobserved: Broker publish failures not observed or flushed

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Kafka; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
A producer `send`/`publish` whose promise or callback is ignored, or a function returning or shutting down before the send is flushed, violates any delivery guarantee the code claims.

## Detect
- `producer.send(...)`, `channel.publish/sendToQueue`, `sqs.send(new SendMessageCommand)`, `sns.publish` not awaited/returned/handled.
- Callback-style publish with no error argument handling.
- `process.exit()`/handler return directly after a non-awaited send, or no `producer.flush/disconnect` on shutdown.

## Do not flag
- Awaited sends inside try/catch that propagate.
- Explicit best-effort telemetry.

## Examples
Flag:
```ts
producer.send({ topic: 't', messages: [m] }); res.sendStatus(202);
```
OK:
```ts
await producer.send({ topic: 't', messages: [m] });
```

## Implementation notes
Anchor to the send call. Overlaps `floating-promise` for TS with types; this rule also covers JS and callbacks.

## Required tests
Unawaited send (finding), awaited (no finding), callback ignoring error (finding), publish confirm channel callback handling error (no finding).

## AI prompt
"Does the code claim durability (acks, confirms, transactions) that the unobserved send cannot provide? Verify against visible acks/retries config. Database and Kafka commits are not atomic." Evidence: send call, producer config, claimed guarantee.

## Why hybrid
Detection is local; whether delivery durability matters comes from the contract.
