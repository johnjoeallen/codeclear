# python/celery-dispatch-before-commit: Task dispatched inside a transaction before it commits

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
`.delay()`/`.apply_async()` inside an open transaction can run before commit: the worker does not find the row (`DoesNotExist`) or processes data that is later rolled back.

## Detect
Candidates: `.delay(`, `.apply_async(`, `send_task(` inside `with transaction.atomic()`, `with session.begin()` or a function decorated with `@transaction.atomic`, with arguments derived from objects created in the same block, and not wrapped in `transaction.on_commit`.

## Do not flag
- Dispatch inside `on_commit(lambda: ...)`, `celery_app.task(...)` with `bind` and `on_commit` helper; dispatch after the block.
- Tasks that do not read data written in the transaction.

## Examples
Flag:
```python
with transaction.atomic():
    order = Order.objects.create(...)
    send_receipt.delay(order.id)
```
OK:
```python
with transaction.atomic():
    order = Order.objects.create(...)
    transaction.on_commit(lambda: send_receipt.delay(order.id))
```

## Implementation notes
Python `ast`; anchor to the dispatch call. Loop-variable capture in the `on_commit` lambda is covered by `late-binding-loop-closure`.

## AI prompt
Question: "Does the task read data written in this transaction, or is its effect incorrect if the transaction rolls back?" Supply the block and the task body. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Dispatch-in-transaction is syntactic; whether the task depends on uncommitted data needs its body.
