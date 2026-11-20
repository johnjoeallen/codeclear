# python/kafka-ordering-and-schema: Partition keys, per-partition concurrency, batch partial failure and schema compatibility

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Kafka; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Ordering required by consumers is broken by a missing/wrong partition key or by processing a partition's records concurrently; batch partial failures are mishandled; changed message schemas break consumers.

## Why AI
Needs ordering requirements, topic partitioning, the consumer concurrency model and visible consumer schemas.

## AI prompt
"Where the code or consumers depend on per-entity ordering, is the key stable per entity and are records of one partition handled sequentially? On a partial batch failure, is order preserved on retry? Is the changed schema compatible with visible consumers?" Supply producer/consumer code, topic config and consumer schemas. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Independent events with no ordering requirement; additive tolerant schema changes.

## Examples
Flag:
```python
producer.send("account-events", value=evt)      # no key; consumer applies balance deltas in order
```
OK:
```python
producer.send("account-events", key=evt.account_id.encode(), value=evt)
```

## Evaluation cases
1. Above flag example -> flag.
2. `ThreadPoolExecutor.map(handle, partition_records)` for order-dependent handling -> flag.
3. Keyed by entity id, sequential handling -> no finding.
4. New required field with shown consumer rejecting unknown schema -> flag.
