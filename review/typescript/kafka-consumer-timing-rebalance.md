# typescript/kafka-consumer-timing-rebalance: Processing time versus heartbeat, session and rebalance

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
Long batch processing without heartbeats exceeds session/poll limits, triggers rebalance, and leaves stale work writing after the partition has moved to another consumer.

## Why AI
Depends on supplied consumer settings (`sessionTimeout`, `rebalanceTimeout`, `heartbeatInterval`), batch size and per-record time.

## AI prompt
"Using the supplied config and per-record processing time, can processing exceed the session or poll limit? Is `heartbeat()` called during long batches? After a rebalance, can stale work still commit or write? If config is not supplied, report nothing." Evidence: consumer config, handler duration estimate, rebalance hooks.

## Do not flag
- Short handlers well within limits; handlers that check `isRunning()`/staleness and call `heartbeat()`.

## Examples
Flag: `eachBatch` of 5000 records, 50 ms each, `sessionTimeout: 30000`, no `heartbeat()`.
OK: same batch with `await heartbeat()` per chunk and `isStale()` check before writes.

## Evaluation cases
1. 250 s batch work vs 30 s session, no heartbeat: flag.
2. Per-message 20 ms, default settings: ok.
3. Config not shown: ok.
