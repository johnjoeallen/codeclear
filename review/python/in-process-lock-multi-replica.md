# python/in-process-lock-multi-replica: In-memory lock or deduplication used across processes or replicas

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Threads, processes, and request context |

## Intent
`threading.Lock`, `asyncio.Lock`, module-level sets and `lru_cache` dedupe only within one process. With several Gunicorn/Celery workers or service replicas they do not provide mutual exclusion or duplicate suppression.

## Why AI
The deployment (worker count, replicas, scheduler) is outside the file; report only when the context shows multiple processes need coordination.

## AI prompt
"Is this in-memory lock/dedup relied on to protect an invariant that spans processes or replicas (per deployment config, worker settings, scheduler)? What happens when two processes run this concurrently?" Supply the code, deployment/worker configuration, and the invariant. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Single-process tools/scripts; locks that only protect process-local state.
- Cross-process coordination via DB unique constraints, row locks or a distributed lock.

## Examples
Flag:
```python
_seen = set()
def handle(msg):
    if msg.id in _seen: return          # 4 gunicorn workers
    _seen.add(msg.id); charge(msg)
```
OK:
```python
INSERT ... ON CONFLICT (msg_id) DO NOTHING   # then charge only if a row was inserted
```

## Evaluation cases
1. Above flag example with `workers = 4` shown -> flag.
2. Same code, deployment shown as one worker with no scale-out -> no finding.
3. `threading.Lock` around a process-local cache refresh -> no finding.
4. APScheduler job guarded by in-memory flag on a service with 3 replicas shown -> flag.
