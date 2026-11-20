# Tests: python/gather-taskgroup-error-semantics

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: gather of writes with no handling (Examples: Flag)

covers: example:flag,required:gather-writes

```python path=app/jobs.py
async def run(order):
    await asyncio.gather(save_order(order), send_email(order))  # CANDIDATE
```

## Case: a try block around it, and reads only (Examples: OK)

covers: example:ok,do-not-flag:handled,required:try

```python path=app/jobs.py
async def a(order):
    try:
        await asyncio.gather(save_order(order), send_email(order))
    except Exception:
        await cleanup(order)

async def b(ids):
    return await asyncio.gather(fetch(1), fetch(2))
```

## Case: return_exceptions=True that is never checked

covers: detect:return-exceptions,do-not-flag:results-checked

```python path=app/jobs.py
async def a(jobs):
    await asyncio.gather(*jobs, return_exceptions=True)  # CANDIDATE

async def b(jobs):
    results = await asyncio.gather(*jobs, return_exceptions=True)
    for r in results:
        if isinstance(r, Exception):
            log.error(r)
```

## Case: a TaskGroup with writes

covers: detect:taskgroup

```python path=app/jobs.py
async def a(order):
    async with asyncio.TaskGroup() as tg:  # CANDIDATE
        tg.create_task(save_order(order))
        tg.create_task(send_email(order))

async def b():
    async with asyncio.TaskGroup() as tg:
        tg.create_task(fetch(1))
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_jobs.py
async def test_it(order):
    await asyncio.gather(save_order(order), send_email(order))
```
