# Tests: python/event-loop-ownership

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: asyncio.run inside a coroutine (Examples: Flag)

covers: example:flag,required:run-in-async

```python path=app/loop.py
async def handler():
    asyncio.run(work())  # CANDIDATE
```

## Case: awaiting, and run at the top (Examples: OK)

covers: example:ok,do-not-flag:await,required:await

```python path=app/loop.py
async def handler():
    await work()

def main():
    asyncio.run(handler())
```

## Case: run_until_complete in a coroutine

covers: detect:run-until-complete

```python path=app/loop.py
async def handler(loop):
    loop.run_until_complete(work())  # CANDIDATE
```

## Case: the loop touched from a thread

covers: detect:thread,required:thread

```python path=app/threads.py
def worker():
    loop.call_soon(callback)  # CANDIDATE
    fut.set_result(1)  # CANDIDATE
    loop.call_soon_threadsafe(callback)

def start():
    Thread(target=worker).start()
```

## Case: thread-safe entry points and a function no thread runs (Do not flag)

covers: do-not-flag:threadsafe

```python path=app/threads.py
def worker():
    loop.call_soon_threadsafe(callback)
    asyncio.run_coroutine_threadsafe(work(), loop)

def not_a_thread():
    loop.call_soon(callback)

def start():
    Thread(target=worker).start()
```

## Case: module-level primitives and several runs

covers: detect:module-level

```python path=app/shared.py
lock = asyncio.Lock()  # CANDIDATE
queue = asyncio.Queue()  # CANDIDATE
def a():
    asyncio.run(one())
def b():
    asyncio.run(two())
```

```python path=app/single.py
lock = asyncio.Lock()
def a():
    asyncio.run(one())
```
