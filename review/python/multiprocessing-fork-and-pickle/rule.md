# python/multiprocessing-fork-and-pickle: Inherited connections, unpicklable arguments and worker failures

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Threads, processes, and request context; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
After `fork`, child processes share the parent's DB/socket connections and import-time state; arguments sent to worker processes (multiprocessing, Celery) must be picklable/serializable; a worker crash or exception may be lost.

## Why AI
Depends on the start method/platform (`fork` vs `spawn`), where connections are created, and the pool/task lifecycle.

## AI prompt
"Given the visible start method and pool lifecycle, does a child process reuse a parent connection/client, depend on import-time side effects, receive unpicklable arguments, or lose a worker failure?" Supply process creation, connection creation sites, and the start method. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Connections created inside the worker (initializer); `spawn` start method; picklable plain data.

## Examples
Flag:
```python
engine = create_engine(URL)          # module level
with Pool(4) as p: p.map(lambda r: engine.execute(r), rows)   # fork shares connection; lambda unpicklable
```
OK:
```python
def init(): global engine; engine = create_engine(URL)
with Pool(4, initializer=init) as p: p.map(work, rows)
```

## Evaluation cases
1. Above flag example -> flag.
2. `Pool.map(work, rows)` with module-level function and no connections -> no finding.
3. `Celery .delay(db_session)` -> flag (unserializable).
4. `apply_async` with no error callback and result discarded where failure must be handled -> flag.
