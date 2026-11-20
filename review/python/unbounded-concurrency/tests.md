# Tests: python/unbounded-concurrency

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: gather over a comprehension (Examples: Flag)

covers: example:flag,required:gather

```python path=app/jobs.py
async def run(urls):
    return await asyncio.gather(*[fetch(u) for u in urls])  # CANDIDATE
```

## Case: bounded by a semaphore inside the task (Examples: OK)

covers: example:ok,do-not-flag:semaphore,do-not-flag:small-collection,required:semaphore

```python path=app/jobs.py
async def run(urls, sem):
    async def one(u):
        async with sem:
            return await fetch(u)
    return await asyncio.gather(*[one(u) for u in [1, 2, 3]])
```

## Case: tasks created in a loop

covers: detect:loop

```python path=app/jobs.py
async def run(items):
    for i in items:
        asyncio.create_task(handle(i))  # CANDIDATE
    for i in range(3):
        asyncio.create_task(handle(i))
    for i in [1, 2]:
        pool.submit(handle, i)

def sync(items):
    return [executor.submit(work, i) for i in items]  # CANDIDATE
```

## Case: queues

covers: detect:queues,do-not-flag:bounded-queue

```python path=app/jobs.py
q = asyncio.Queue()  # CANDIDATE
r = queue.Queue(maxsize=0)  # CANDIDATE
ok = asyncio.Queue(maxsize=100)
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_jobs.py
q = asyncio.Queue()
```
