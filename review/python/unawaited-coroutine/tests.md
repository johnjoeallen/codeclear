# Tests: python/unawaited-coroutine

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a coroutine called without await (Examples: Flag)

covers: example:flag,required:no-await

```python path=app/jobs.py
async def fetch():
    return 1

async def main():
    fetch()  # CANDIDATE
    asyncio.sleep(1)  # CANDIDATE
```

## Case: awaited or stored (Examples: OK)

covers: example:ok,do-not-flag:stored-task,required:awaited

```python path=app/jobs.py
async def fetch():
    return 1

async def main():
    await fetch()
    await asyncio.sleep(1)
    task = asyncio.create_task(fetch())
    results = await asyncio.gather(fetch(), fetch())
```

## Case: gather, wait_for and a method

covers: detect:asyncio-calls

```python path=app/jobs.py
class Worker:
    async def run(self):
        pass
    async def start(self):
        asyncio.gather(self.run())  # CANDIDATE
        asyncio.wait_for(self.run(), 1)  # CANDIDATE
        self.run()  # CANDIDATE
```

## Case: a task created and thrown away

covers: detect:discarded-task,required:discarded-task

```python path=app/jobs.py
async def main():
    asyncio.create_task(work())  # CANDIDATE
    asyncio.ensure_future(work())  # CANDIDATE
```

## Case: a coroutine returned to an awaiting caller, and a sync function (Do not flag)

covers: do-not-flag:returned

```python path=app/jobs.py
async def work():
    pass

def make():
    return work()

def sync():
    helper()
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_jobs.py
async def test_it():
    asyncio.sleep(1)
```
