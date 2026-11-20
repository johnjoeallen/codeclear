# python/kafka-redelivery-idempotency: Non-idempotent effects under Kafka redelivery

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Kafka; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Redelivery is possible (rebalance, crash before commit). Effects such as inserts, payments or emails duplicate unless deduplicated. Producer idempotence does not deduplicate database writes, and Kafka transactions do not include a separate database.

## Why AI
Needs the effect's idempotency (keys, constraints) and the actual delivery configuration.

## AI prompt
"Assume this message is delivered twice. Does the handler produce a duplicate or conflicting effect? Is a claimed exactly-once property real given that the database is outside the Kafka transaction?" Supply the handler, DB constraints/dedup logic, producer/consumer config. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Upserts, unique message-id constraints, idempotent external calls with keys.

## Examples
Flag:
```python
def handle(msg): db.execute("INSERT INTO payments(order_id, amt) VALUES (%s,%s)", ...)
```
OK:
```python
db.execute("INSERT ... ON CONFLICT (msg_id) DO NOTHING", ...)
```

## Evaluation cases
1. Above flag example without unique index on `order_id` -> flag.
2. `enable.idempotence=true` cited as protection for a DB insert -> flag.
3. Insert guarded by `ON CONFLICT (msg_id)` -> no finding.
4. Handler only updates a cache entry with the same value -> no finding.
