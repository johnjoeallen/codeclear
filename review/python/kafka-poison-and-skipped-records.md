# python/kafka-poison-and-skipped-records: Swallowed failures, poison-message loops and offsets advancing past failed records

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Kafka; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
A consumer catches an exception and moves on (record silently skipped), retries a poison record forever, or in a batch commits the offset of the last record although earlier ones failed.

## Why AI
Whether skipping is acceptable depends on the DLQ/retry design, batch semantics and the client's exception behavior.

## AI prompt
"For a record whose processing fails permanently, what happens: is it retried forever, dropped, dead-lettered, or does a later offset commit cover it? In a batch, which offsets are committed relative to failed records?" Supply the consume/handle code, exception handling, DLQ and commit code. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Failures routed to a visible DLQ/retry topic with bounded retries and alerting.

## Examples
Flag:
```python
for msg in batch:
    try: handle(msg)
    except Exception: log.error("failed")
consumer.commit()                      # failed records lost
```
OK:
```python
except Exception: dlq.send(msg); 
```

## Evaluation cases
1. Above flag example -> flag.
2. `while True: try: handle(msg); break except: sleep(1)` with no limit on a permanently bad payload -> flag.
3. Failure -> DLQ publish with ack awaited, then commit -> no finding.
4. Batch commit of `batch[-1].offset+1` after partial failure -> flag.
