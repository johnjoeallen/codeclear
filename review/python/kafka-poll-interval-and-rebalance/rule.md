# python/kafka-poll-interval-and-rebalance: Processing time versus poll/heartbeat limits and rebalance cleanup

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Kafka; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Handling that exceeds `max.poll.interval.ms` or blocks heartbeats gets the consumer evicted, causing rebalances and duplicate processing; partition-owned state is not cleaned or committed on revoke.

## Why AI
Depends on visible poll/heartbeat settings, worst-case processing time, and sync vs async client semantics.

## AI prompt
"Compare worst-case time between `poll()` calls (including retries and blocking calls) with the configured limits. On rebalance/revoke, is in-flight work committed or discarded and partition state cleaned?" Supply the loop, client config and any rebalance listener. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Processing demonstrably well under limits; pause/resume or a separate heartbeat thread shown.

## Examples
Flag:
```python
for msg in consumer:             # max_poll_interval_ms=300000
    time.sleep(600)              # exceeds interval -> evicted, redelivered
```
OK:
```python
consumer.pause(parts); future = pool.submit(handle, msg); # keep polling while pending
```

## Evaluation cases
1. Above flag example -> flag.
2. Handler issues retries with up to 10 min total and default 5 min poll interval -> flag.
3. Handler is a quick DB upsert -> no finding.
4. `on_revoke` commits offsets of processed records and clears local buffers -> no finding.
