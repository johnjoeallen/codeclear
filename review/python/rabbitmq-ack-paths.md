# python/rabbitmq-ack-paths: Manual ack/nack/reject not matched to processing and durable commit

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Acking before processing/commit loses messages on crash; exception paths that neither ack nor nack leave deliveries unacked, exhausting prefetch and stalling the consumer; channel failure paths drop acks.

## Why AI
Needs every ack path against the DB commit order, prefetch settings and the client (pika, aio-pika, kombu).

## AI prompt
"Trace every path (success, each exception, channel failure) to ack/nack/reject, relative to the durable commit. Which path loses the message, leaves it unacked, or acks before commit?" Supply the callback, prefetch/QoS settings and DB code. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- `auto_ack=True` consumers where at-most-once is intended; ack in `finally` after durable handling with failures nacked.

## Examples
Flag:
```python
def cb(ch, method, props, body):
    process(body)               # raises -> no ack/nack, prefetch consumed
    ch.basic_ack(method.delivery_tag)
```
OK:
```python
try: process(body); ch.basic_ack(method.delivery_tag)
except Exception: ch.basic_nack(method.delivery_tag, requeue=False); raise
```

## Evaluation cases
1. Above flag example -> flag.
2. `basic_ack` before `db.commit()` -> flag.
3. Ack/nack on every path, ack after commit -> no finding.
4. `aio_pika` `message.process()` context manager (requeue off) wrapping work -> no finding.
