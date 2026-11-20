# python/cache-stampede-and-distributed-lock: Cache stampedes, negative caching, lock ownership, lease expiry and fencing

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Caches and distributed locks; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Many concurrent misses recompute an expensive value or overload the database; negative results are cached too long or not at all; a distributed lock is released by a non-owner or expires while work continues, letting a stale worker write after a new holder took over.

## Why AI
Fencing and stampede protection matter only if realistic concurrency or a stale worker can violate a protected invariant or exhaust capacity.

## AI prompt
"Trace lock ownership, lease expiry, renewal, stale writes and release. Can a worker whose lease expired still write? Is release owner-checked? Can concurrent misses exhaust capacity or cache a negative result incorrectly?" Supply lock/cache code, lease/TTL, protected operation and load facts. Report only with a concrete trigger scenario; require fencing only with an evidenced stale-worker violation; otherwise NO_FINDING.

## Do not flag
- Owner-token release (Lua compare-and-delete), fencing tokens checked by the resource, or idempotent recomputation of cheap values.

## Examples
Flag:
```python
if r.set("lock", "1", nx=True, ex=10):
    slow_update(); r.delete("lock")         # runs 30 s, deletes another holder's lock
```
OK:
```python
token = uuid4().hex; r.set("lock", token, nx=True, ex=10); ...; release_if_owner("lock", token)
```

## Evaluation cases
1. Above flag example -> flag.
2. Lock with owner token and fencing token checked by the DB write -> no finding.
3. Hot key expiry recomputed by 500 concurrent requests against a slow query -> flag.
4. Cheap local computation cached per process -> no finding.
