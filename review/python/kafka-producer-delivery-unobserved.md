# python/kafka-producer-delivery-unobserved: Producer send without observing delivery or flushing

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Kafka |

## Intent
`producer.send()`/`produce()` is asynchronous. Ignoring the returned future/delivery callback and not flushing before exit or before reporting success silently loses failed or unsent messages.

## Detect
Candidates: calls to `send`, `produce` (kafka-python, confluent-kafka, aiokafka `send` not awaited/`send_and_wait`) whose result is discarded, with no `on_delivery`/`add_errback`/`.get()`, and no `flush()` (or `stop()`/`close()`) on the same function/exit path.

## Do not flag
- `send_and_wait`, `future.get(timeout=...)`, delivery callbacks that handle errors, `flush()` before returning.
- Fire-and-forget telemetry documented as best-effort.

## Examples
Flag:
```python
producer.send("orders", value)
return {"status": "queued"}
```
OK:
```python
producer.send("orders", value).get(timeout=10)
```

## Implementation notes
Python `ast`; anchor to the produce call.

## AI prompt
Question: "Does the caller report success or the process exit while delivery may have failed, given the visible `acks`/retries settings?" Supply the producer config and the surrounding function. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The unobserved send is syntactic; whether loss matters depends on the delivery requirement and acks config.
