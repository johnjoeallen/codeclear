# python/celery-subtask-wait-starvation: Task blocks waiting for subtasks, chords or groups

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
A Celery task that calls `.get()`/`.join()`/`.wait()` on subtasks occupies a worker slot while waiting for work that needs a slot from the same pool, deadlocking or starving when the pool is full.

## Detect
Candidates: inside a function decorated with `@app.task`/`@shared_task`/`@task`, a call `.get(`, `.join(`, `.collect(` on the result of `.delay`, `.apply_async`, `group(...)()`, or `chord(...)`; `.get(disable_sync_subtasks=False)` is always a finding.

## Do not flag
- `apply()` (eager local execution); `.get()` outside a task; use of `chord`/`link` callbacks instead of waiting.

## Examples
Flag:
```python
@app.task
def parent(ids):
    return [child.delay(i).get() for i in ids]
```
OK:
```python
@app.task
def parent(ids):
    chord(child.s(i) for i in ids)(collect.s())
```

## Implementation notes
Python `ast`; anchor to the waiting call. Celery already warns about this by default.

## AI prompt
Question: "Does the visible worker/pool/queue topology let the child tasks run while the parent holds a slot, or can the pool fill with parents?" Supply worker config, queues and concurrency. Report only with a concrete trigger scenario; do not assume the pool or result backend; otherwise NO_FINDING.

## Why hybrid
The wait is syntactic; whether it deadlocks depends on pool configuration.
