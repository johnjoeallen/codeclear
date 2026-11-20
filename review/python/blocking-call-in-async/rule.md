# python/blocking-call-in-async: Blocking call inside async def

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | python-review: Asyncio and background work |

## Intent
Blocking calls inside a coroutine stall the whole event loop, so every other request waits.

## Detect
Inside an `async def` body (excluding nested sync functions), a call to:
- `time.sleep`
- `requests.*`, `urllib.request.urlopen`
- `subprocess.run|call|check_output` without `await asyncio.to_thread`
- builtin `open()` for large reads, `os.system`
- synchronous database drivers (`psycopg2`, `pymysql`, `sqlite3.connect().execute`)
- calls on a synchronous `boto3` client/resource created in the same module (`boto3.client(...)`, `boto3.resource(...)`)

## Do not flag
- The call is the argument of `asyncio.to_thread(...)`, `loop.run_in_executor(...)` or `anyio.to_thread.run_sync(...)`.
- Calls in nested non-async functions.
- A framework that offloads sync handlers to a thread pool (plain `def` endpoints).

## Examples
Flag:
```python
async def handler():
    time.sleep(1)
```
OK:
```python
async def handler():
    await asyncio.sleep(1)
```

## Implementation notes
Python `ast` with a configurable blocklist. Equivalent: Ruff `ASYNC` rules (`ASYNC210`, `ASYNC251`).

## Why deterministic
Enclosing `async def` and a blocklisted callee are syntactic facts; offloading wrappers are excluded by shape.

The nearest enclosing function of the call must be an `async def`; a lambda or a plain nested `def` ends the search. The blocklist is `time.sleep`, `requests.*`, `urllib.request.urlopen`, `os.system`, `subprocess.run/call/check_call/check_output/Popen`, and `connect` of `psycopg2`, `pymysql`, `sqlite3` and `MySQLdb`, plus calls on a name the module assigns from `boto3.client(...)` or `boto3.resource(...)`. The builtin `open()` is not flagged, since the rule is about large reads and the size is not known.

## Required tests
`time.sleep` (finding), `requests.get` (finding), `to_thread` wrapped (no finding), sync function (no finding).
