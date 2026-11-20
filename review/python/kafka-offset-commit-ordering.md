# python/kafka-offset-commit-ordering: Offset commit not ordered after processing and durable commit

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Kafka; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Committing an offset (auto-commit, `commit()` or `store_offsets`) before processing and the database commit finish loses messages on a crash; committing long after causes mass reprocessing.

## Why AI
Needs the actual client (kafka-python, confluent-kafka, aiokafka) commit/batch/exception semantics, its configuration (`enable.auto.commit`) and the database commit point.

## AI prompt
"Draw the order: poll -> process -> database commit -> offset commit using the client's real settings. Inject failure at each boundary: is the outcome loss, redelivery or partial success, and which is unintended?" Supply the consumer loop, client config and DB code. Report only with a concrete trigger scenario (failure point, outcome); do not infer guarantees from the callback alone; otherwise NO_FINDING.

## Do not flag
- Commit after durable processing with idempotent handling; at-most-once explicitly intended and documented.

## Examples
Flag:
```python
for msg in consumer:                 # enable_auto_commit=True
    save(msg)                        # crash after auto-commit, before save finishes
```
OK:
```python
for msg in consumer:
    save(msg); consumer.commit()
```

## Evaluation cases
1. Above flag example with auto-commit interval 5 s and async `save` -> flag.
2. `commit()` called before `db.commit()` -> flag.
3. `enable.auto.commit=False`, `commit(asynchronous=False)` after DB commit -> no finding.
4. Documented at-most-once metrics consumer -> no finding.
