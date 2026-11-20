# Tests: python/kafka-producer-delivery-unobserved

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: send with the result thrown away (Examples: Flag)

covers: example:flag,required:discarded

```python path=app/events.py
def publish(event):
    producer.send("orders", event)  # CANDIDATE
```

## Case: a delivery callback, and a flush (Examples: OK)

covers: example:ok,do-not-flag:callback-or-flush,required:callback

```python path=app/events.py
def a(event):
    producer.produce("orders", event, on_delivery=report)

def b(event):
    producer.send("orders", event)
    producer.flush()

def c(event):
    future = producer.send("orders", event)
    future.add_errback(log_error)
```

## Case: produce, and a producer on self

covers: detect:forms

```python path=app/events.py
class Publisher:
    def a(self, event):
        self.producer.produce("orders", event)  # CANDIDATE
    def b(self, event):
        self.kafka_producer.send("orders", event)  # CANDIDATE
```

## Case: the future is kept, and other receivers (Do not flag)

covers: do-not-flag:result-kept

```python path=app/events.py
def a(event):
    future = producer.send("orders", event)
    return future

def b(sock, data):
    sock.send(data)
    channel.send(data)
```

## Case: stop or close on the same path

covers: detect:close

```python path=app/events.py
def a(event):
    producer.send("orders", event)
    producer.close()
```
