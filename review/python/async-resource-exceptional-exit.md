# python/async-resource-exceptional-exit: Async generators, clients and locks not cleaned up on cancel or error

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Asyncio and background work; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
Async generators, async context managers, clients and locks leak or stay held when the consumer stops early, an exception occurs or the task is cancelled; a generator/stream iterated after its handler or session exited reads closed resources or writes outside the intended transaction. `shield` does not replace observing the task's completion.

## Why AI
Needs the lifetime of the resource relative to the consumer (response streaming, session scope), framework teardown order and cancellation points.

## AI prompt
"On early exit, exception or cancellation, are the async generator, client, lock and session closed/released, and is any deferred iteration executed after its resource or transaction has ended?" Supply the generator/stream, the handler that returns it, and dependency teardown. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Resources managed by `async with`/`try/finally`/`aclosing`, or framework-owned lifecycle shown to cover the path.

## Examples
Flag:
```python
async def stream(db=Depends(get_db)):
    async def gen():
        async for r in db.stream(q): yield r     # runs after db closed
    return StreamingResponse(gen())
```
OK:
```python
async def gen():
    async with sessionmaker() as s:
        async for r in s.stream(q): yield r
```

## Evaluation cases
1. Above flag example -> flag.
2. `lock.acquire()` ... `await x` ... `lock.release()` without `finally` -> flag.
3. `async with aclosing(gen()) as g` -> no finding.
4. `asyncio.shield(task)` then outer cancelled and the task result never awaited -> flag.
