# python/background-task-ownership-shutdown: Detached task not owned, awaited or shut down

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Asyncio and background work; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
Background work started with `create_task`, `ensure_future` or a thread is not durable: nobody retains it, observes its exception, cancels or drains it on shutdown, or it uses request-scoped dependencies that are already closed when it runs. The deterministic discarded-reference case is in `unawaited-coroutine`.

## Why AI
Ownership and shutdown behavior depend on application lifecycle code elsewhere, and whether the work must complete depends on the business requirement.

## AI prompt
"Who retains and observes this task? On shutdown or handler return, is the work cancelled, drained or lost, and does it touch a session/connection/client that is closed by then?" Supply the code that starts the task, lifespan/shutdown handlers, and the dependencies it uses. Report only with a concrete trigger scenario (shutdown/return timing, lost work or exception, changed line); otherwise NO_FINDING.

## Do not flag
- Fire-and-forget work documented as best-effort (metrics, logging).
- Tasks stored in a supervised set with done-callbacks and drained on shutdown.

## Examples
Flag:
```python
@app.post("/orders")
async def create(o: Order, db=Depends(get_db)):
    asyncio.create_task(notify(o, db))     # db closed after response
    return ok
```
OK:
```python
tasks.add(t := asyncio.create_task(notify(o))); t.add_done_callback(tasks.discard)
```

## Evaluation cases
1. Above flag example -> flag.
2. Task stored and awaited in `lifespan` shutdown -> no finding.
3. `create_task(send_invoice())` where invoice must be sent and no retry exists -> flag.
4. `create_task(log_metric())` best-effort -> no finding.
