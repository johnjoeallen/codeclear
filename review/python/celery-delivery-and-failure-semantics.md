# python/celery-delivery-and-failure-semantics: Celery delivery, execution, retry, result and worker-loss semantics

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
Misreading Celery semantics: acks happen before execution by default (loss on worker death unless `acks_late`), `autoretry_for`/`self.retry` raise control flow that skips code and repeats non-idempotent effects, swallowed task exceptions mark SUCCESS, retries are unbounded, task arguments are not serializable or hold request-scoped resources, and results/chords fail silently.

## Why AI
Depends on visible `acks_late`, `task_reject_on_worker_lost`, prefetch, retry and result-backend settings; do not assert missing configuration from a task file alone.

## AI prompt
"Using the visible settings, trace: worker crash mid-task, task exception, `self.retry` (control flow and bounded `max_retries`), retry of non-idempotent effects, retry exhaustion, and shutdown. What state and result does the system end in?" Supply the task, Celery config, serializers and callers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Idempotent tasks; tasks documented at-most-once; configuration not shown.

## Examples
Flag:
```python
@app.task(bind=True, max_retries=None)
def pay(self, id):
    try: charge(id)
    except Exception as e: raise self.retry(exc=e)      # unbounded, charge repeated
```
OK:
```python
@app.task(bind=True, max_retries=5, acks_late=True)
def pay(self, id): charge(id, idempotency_key=id)
```

## Evaluation cases
1. Above flag example -> flag.
2. `except Exception: log.error(...)` in a task whose failure must be visible -> flag.
3. `.delay(db_session_obj)` or a model instance as an argument with the JSON serializer -> flag.
4. Idempotent task with `acks_late`, `reject_on_worker_lost`, bounded retries -> no finding.
