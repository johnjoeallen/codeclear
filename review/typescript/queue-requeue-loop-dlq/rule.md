# typescript/queue-requeue-loop-dlq: Permanent failures requeued forever or without dead-letter routing

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: RabbitMQ and background jobs; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
`nack(msg, false, true)` / `reject(msg, true)` on a message that always fails creates a hot redelivery loop. Retries need a bound and a dead-letter destination.

## Detect
`nack(m, ..., true)`, `reject(m, true)` or `requeue: true` in a catch/error branch with no retry counter (`x-death`, header attempts, delay queue) in the callback, or a queue assertion without `deadLetterExchange` for a queue consumed with nack/reject.

## Do not flag
- Requeue only for known transient errors with a bound.
- Queues whose DLX is declared in shown config or infra.

## Examples
Flag:
```ts
catch { ch.nack(m, false, true); }
```
OK:
```ts
catch { ch.nack(m, false, attempts(m) < 3); } // false routes to the DLX
```

## Implementation notes
Anchor to the nack/reject.

## Required tests
Unconditional requeue (finding), conditional with attempts counter (no finding), `requeue=false` (no finding).

## AI prompt
"Is the failure permanent, and what bounds or dead-letters the retry? Do not assume a DLX is absent when its declaration is not supplied." Evidence: consumer, queue declaration/policies, infra config.

## Why hybrid
The requeue pattern is syntactic; dead-letter configuration is often external.
