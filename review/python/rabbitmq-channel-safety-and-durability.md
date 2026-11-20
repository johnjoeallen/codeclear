# python/rabbitmq-channel-safety-and-durability: Channel/connection thread safety, durability and lifecycle

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs |

## Intent
Pika `BlockingConnection`/channels are not thread-safe; non-durable queues, transient messages or missing TTL choices lose data; connections/channels are not reconnected or closed on failure.

## Why AI
Needs the threading model and the durability requirement of the messages.

## AI prompt
"Are connections/channels shared across threads, are queues/messages durable where messages must survive a broker restart, and are TTL and connection lifecycle (reconnect, close) handled?" Supply the connection setup, declarations, publish/consume code and threading. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- One channel per thread; transient messages for ephemeral data; thread-safe wrappers (`add_callback_threadsafe`).

## Examples
Flag:
```python
channel = conn.channel()                     # module level
threading.Thread(target=lambda: channel.basic_publish(...)).start()
```
OK:
```python
conn.add_callback_threadsafe(lambda: channel.basic_publish(...))
```

## Evaluation cases
1. Above flag example -> flag.
2. `queue_declare(queue="payments")` (non-durable) with persistent business messages -> flag.
3. Durable queue + `delivery_mode=2` -> no finding.
4. Connection never reconnected after `AMQPConnectionError` in a long-running consumer -> flag.
