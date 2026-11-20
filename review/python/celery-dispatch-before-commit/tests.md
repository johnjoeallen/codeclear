# Tests: python/celery-dispatch-before-commit

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: .delay inside transaction.atomic (Examples: Flag)

covers: example:flag,required:delay-in-atomic

```python path=app/orders.py
def create(data):
    with transaction.atomic():
        order = Order.objects.create(**data)
        send_receipt.delay(order.id)  # CANDIDATE
```

## Case: on_commit (Examples: OK)

covers: example:ok,do-not-flag:on-commit,required:on-commit

```python path=app/orders.py
def create(data):
    with transaction.atomic():
        order = Order.objects.create(**data)
        transaction.on_commit(lambda: send_receipt.delay(order.id))
        transaction.on_commit(partial(send_receipt.delay, order.id))
```

## Case: apply_async, send_task and a decorated function

covers: detect:forms

```python path=app/orders.py
@transaction.atomic
def a(order):
    order.save()
    notify.apply_async(args=[order.id])  # CANDIDATE
    app.send_task("tasks.notify", args=[order.id])  # CANDIDATE
```

## Case: a dispatch outside any transaction, and one with constant arguments (Do not flag)

covers: do-not-flag:no-transaction

```python path=app/orders.py
def a(order):
    order.save()
    send_receipt.delay(order.id)

def b():
    with transaction.atomic():
        cleanup.delay()
        ping.delay(1, "x")
```

## Case: a session transaction

covers: detect:session

```python path=app/orders.py
def a(session, order):
    with session.begin():
        session.add(order)
        notify.delay(order.id)  # CANDIDATE
```
