# python/rabbitmq-poison-dlq-and-confirms: Poison requeue loops, retry bounds, dead-lettering and publisher confirms

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
`nack(requeue=True)` on a permanently failing message loops forever; retries lack bounds or dead-letter routing; publishes lack confirms/mandatory handling so unroutable or unconfirmed messages are lost; a retry-publish failure drops the message. Publisher confirmation does not prove consumer processing or a DB commit.

## Why AI
Depends on broker/queue configuration (DLX, TTL, policies) that may not be in the file; must not assert absent DLQ from code alone.

## AI prompt
"For a message that always fails, what happens: requeue loop, bounded retries, DLQ? If the retry publish fails, is the original acked and lost? Are confirms/mandatory used where delivery matters?" Supply consumer, publish code, and queue/exchange declarations or policies. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Queue declared with a DLX and bounded delivery limit (quorum queue `x-delivery-limit`); best-effort notifications.

## Examples
Flag:
```python
except Exception: ch.basic_nack(tag, requeue=True)     # poison message loops forever
```
OK:
```python
except Exception: ch.basic_nack(tag, requeue=False)    # queue has x-dead-letter-exchange
```

## Evaluation cases
1. Above flag example, no delivery limit shown -> flag.
2. `ch.basic_publish(...)` for an order event without `confirm_delivery()` and no mandatory flag -> flag.
3. Queue with `x-dead-letter-exchange` and `requeue=False` -> no finding.
4. Retry via republish then `basic_ack` where republish exceptions are swallowed -> flag.
