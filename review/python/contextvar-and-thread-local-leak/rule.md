# python/contextvar-and-thread-local-leak: Request or tenant context leaks or disappears across tasks and threads

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Threads, processes, and request context; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
A `ContextVar`/thread-local set without a reset leaks to the next request on a reused worker; executors and threads do not inherit context by default, so tenant/user state disappears; copied contexts still share one mutable object.

## Why AI
Needs the framework's thread/context reuse model and where the value is set, reset and read.

## AI prompt
"Is a `ContextVar.set()`/thread-local assignment reset on every exit path? Is the value read in a thread/executor/new task where it is not propagated? Do two contexts share a mutable value?" Supply set/reset/read sites and how work is dispatched. Report only with a concrete trigger scenario (e.g. request B sees tenant A); otherwise NO_FINDING.

## Do not flag
- `token = var.set(x)` with `var.reset(token)` in `finally`; `copy_context().run`/`to_thread` propagation shown; immutable values.

## Examples
Flag:
```python
tenant_var.set(req.tenant)          # never reset; worker reused
```
OK:
```python
token = tenant_var.set(req.tenant)
try: ...
finally: tenant_var.reset(token)
```

## Evaluation cases
1. Above flag example -> flag.
2. Tenant read inside `ThreadPoolExecutor.submit` callable using `threading.local` set in the request thread -> flag.
3. `asyncio.to_thread` reading a ContextVar -> no finding (propagates).
4. ContextVar holding a shared dict mutated by two tasks -> flag.
