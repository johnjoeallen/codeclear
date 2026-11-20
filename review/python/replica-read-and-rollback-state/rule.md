# python/replica-read-and-rollback-state: Read-after-write on replicas and in-memory state after rollback

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Database invariants and transaction execution |

## Intent
Reading immediately after a write from a lagging replica returns stale data; after a rollback, in-memory models, caches and generated IDs remain changed and are still used by callers that assume persistence.

## Why AI
Needs routing configuration (read replicas, DB routers), consistency requirements and caller behavior after the failure.

## AI prompt
"Does the caller rely on a read-your-writes guarantee that a replica route would break, or use an in-memory model/cache/generated id after a rollback as if it were persisted?" Supply the write/read paths, DB routing, and post-rollback code. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Reads explicitly routed to primary; objects discarded after rollback; eventual consistency accepted in context.

## Examples
Flag:
```python
Order.objects.using("default").create(...)
return Order.objects.using("replica").get(pk=pk)     # may not exist yet
```
OK:
```python
return Order.objects.using("default").get(pk=pk)
```

## Evaluation cases
1. Above flag example -> flag.
2. `session.rollback()` then `cache.set(user.id, user)` with the uncommitted user -> flag.
3. Replica read for a dashboard where staleness is accepted -> no finding.
4. Generated id from rolled-back insert returned to the client -> flag.
