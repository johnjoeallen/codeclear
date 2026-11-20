# python/event-loop-ownership: Nested asyncio.run or cross-thread access to loop-bound objects

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Asyncio and background work; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
`asyncio.run()`/`loop.run_until_complete()` called where a loop is already running raises `RuntimeError`; loop-bound objects (futures, locks, queues, clients) used from another thread or loop break; async/sync adapters block or deadlock the loop.

## Detect
Candidates:
- `asyncio.run(...)` or `run_until_complete` inside an `async def` or in code called from one;
- `loop.call_soon`/`create_task`/`Future.set_result` invoked from a `threading.Thread`/executor target instead of `call_soon_threadsafe`/`run_coroutine_threadsafe`;
- module-level `asyncio.Lock()/Queue()` shared across several `asyncio.run` calls.

## Do not flag
- `asyncio.run` in a top-level `main()` / `if __name__ == "__main__"`.
- `run_coroutine_threadsafe`, `call_soon_threadsafe`.

## Examples
Flag:
```python
async def handler():
    return asyncio.run(fetch())
```
OK:
```python
async def handler():
    return await fetch()
```

## Implementation notes
Python `ast`; anchor to the call.

## AI prompt
Question: "Does the real caller already run an event loop, or does another thread touch loop-bound objects shown here?" Supply the call chain to the entry point. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The call is deterministic; whether a loop is running depends on the caller.
