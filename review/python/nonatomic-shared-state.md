# python/nonatomic-shared-state: Shared read-modify-write, check-then-act or lazy init without synchronization

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Threads, processes, and request context; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
The GIL does not make multi-step business invariants atomic. `counter += 1`, `if k not in cache: cache[k] = ...`, lazy singletons, and shared sessions/clients/collections mutated by concurrent threads or tasks interleave and break the invariant.

## Why AI
Needs the concurrency model (threads, asyncio awaits between steps, worker count), what is shared, and whether the client/session is documented thread-safe.

## AI prompt
"Construct a concrete interleaving of two threads/tasks/requests through this shared state (including `await` points) that violates a stated invariant. Does the code or the client's documented guarantees prevent it?" Supply the shared object's definition and lifetime, the access sites, and the concurrency setup. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- State guarded by a lock held across the whole step, thread-local/contextvar state, or single-step atomic operations (`dict.setdefault`, `queue.Queue`).
- Clients documented thread-safe (e.g. boto3 clients) or objects created per request.

## Examples
Flag:
```python
async def take(n):
    if stock[sku] >= n:
        await audit(sku)
        stock[sku] -= n
```
OK:
```python
async with lock:
    if stock[sku] >= n: stock[sku] -= n
```

## Evaluation cases
1. Above flag example (await between check and act, concurrent requests) -> flag.
2. One module-level SQLAlchemy `Session` used by several threads -> flag.
3. `lock` held around the entire check+update -> no finding.
4. Lazy init `if _c is None: _c = make()` in a script run single-threaded -> no finding.
