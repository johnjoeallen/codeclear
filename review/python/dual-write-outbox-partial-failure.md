# python/dual-write-outbox-partial-failure: Database plus broker/cache/search write without a safe failure window

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Microservices and distributed side effects; python-bug-hunter: Distributed workflows and microservices |

## Intent
Writing to a database and then to a broker, cache or search index (or the reverse) leaves a window where a crash or failure makes the stores diverge. A local transaction does not include the external effect, and outbox dispatch itself can redeliver.

## Why AI
Whether the window matters depends on the consistency requirement and on shown outbox, compensation, retry or reconciliation mechanisms.

## AI prompt
"List the sequence of writes. Inject failure/crash before and after each. What durable state remains, and which shown mechanism (outbox, retry, reconciliation, idempotent consumer) recovers it? Under commit-then-crash, send-then-crash and concurrent retry, is an event lost, duplicated or sent for rolled-back data?" Supply the code, consumers, outbox/relay and the consistency requirement. Report only with a concrete trigger scenario; do not require an outbox without one; otherwise NO_FINDING.

## Do not flag
- Best-effort notifications/caches that self-heal; outbox row written in the same transaction with an idempotent consumer.

## Examples
Flag:
```python
with transaction.atomic():
    order.save()
producer.send("orders", order.to_json())     # crash here loses the event
```
OK:
```python
with transaction.atomic():
    order.save(); Outbox.objects.create(topic="orders", payload=order.to_json())
```

## Evaluation cases
1. Above flag example where downstream must see every order -> flag.
2. `producer.send(...)` inside the transaction before commit, then commit fails -> flag.
3. Cache invalidation after commit with a short TTL fallback -> no finding.
4. Outbox written in the transaction and relay marks sent only after broker ack, idempotent consumer shown -> no finding.
