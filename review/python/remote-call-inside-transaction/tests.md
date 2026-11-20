# Tests: python/remote-call-inside-transaction

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an HTTP call inside transaction.atomic (Examples: Flag)

covers: example:flag,required:in-atomic

```python path=app/orders.py
def pay(order):
    with transaction.atomic():
        order.save()
        requests.post(URL, json={"id": order.id})  # CANDIDATE
```

## Case: work after the commit (Examples: OK)

covers: example:ok,do-not-flag:on-commit,required:on-commit

```python path=app/orders.py
def pay(order):
    with transaction.atomic():
        order.save()
        transaction.on_commit(lambda: send_receipt.delay(order.id))
    requests.post(URL, json={"id": order.id})
```

## Case: a decorated function, and the other remote calls

covers: detect:decorated,required:celery

```python path=app/orders.py
@transaction.atomic
def a(order):
    order.save()
    send_receipt.delay(order.id)  # CANDIDATE
    time.sleep(1)  # CANDIDATE
    producer.send("topic", b"x")  # CANDIDATE
    channel.publish(msg)  # CANDIDATE
    client.get(url)  # CANDIDATE
```

## Case: a session transaction

covers: detect:session

```python path=app/orders.py
def a(session):
    with session.begin():
        httpx.get(URL)  # CANDIDATE
```

## Case: calls outside any transaction (Do not flag)

covers: do-not-flag:no-transaction

```python path=app/orders.py
def a(order):
    requests.post(URL)
    with open("f") as fh:
        requests.get(URL)
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_orders.py
def test_it():
    with transaction.atomic():
        requests.get(URL)
```
