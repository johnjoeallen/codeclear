# python/handler-execution-and-lifespan: Sync/async handler semantics, lifespan clients and dependency cleanup

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Web frameworks and backend boundaries; python-bug-hunter: Trace the execution before judging it |

## Intent
Handlers run under different execution models (WSGI thread, ASGI loop, thread-pool offload of plain `def`). Misusing them, creating clients per request instead of in lifespan, or relying on dependency teardown timing leaks connections or blocks the loop; decorators/annotations do not imply validation or transaction interception.

## Why AI
Depends on framework version, how dependencies are declared (yield dependencies, teardown order) and which entry point executes the code (handler, command, task, consumer).

## AI prompt
"Identify the execution owner of this code. Under the framework's semantics, does the handler block the loop, share a non-thread-safe client, create/close a client incorrectly, or depend on a dependency being alive after teardown?" Supply handler, app lifespan/startup code, dependencies and framework version. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Plain `def` endpoints that the framework runs in a thread pool; clients created once in lifespan and closed on shutdown.

## Examples
Flag:
```python
@app.get("/x")
async def x():
    async with httpx.AsyncClient() as c: ...      # new pool per request in a hot path, no limits
```
OK:
```python
@asynccontextmanager
async def lifespan(app): app.state.http = httpx.AsyncClient(); yield; await app.state.http.aclose()
```

## Evaluation cases
1. `async def` FastAPI endpoint calling blocking SDK -> flag (see `blocking-call-in-async`).
2. Yield-dependency session closed, then used by a `BackgroundTasks` function -> flag.
3. `def` endpoint using `requests` -> no finding.
4. Client built in lifespan and shared, documented concurrency-safe -> no finding.
