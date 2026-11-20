# python/cache-key-isolation-and-ttl: Cache key isolation, TTL units, invalidation timing and staleness

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Caches and distributed locks |

## Intent
Redis/cache keys that omit tenant/user lead to cross-user data; TTL given in the wrong unit (`ex` seconds vs `px` ms, Django seconds); invalidation before the write commits repopulates stale data; stale values violate a stronger consistency requirement.

## Why AI
Needs the key's data scope, the client's TTL units and the consistency requirement of the readers.

## AI prompt
"Does the cache key include every dimension (tenant, user, locale, version) the value depends on? Is the TTL in the client's unit? Is invalidation ordered after the commit, and can a reader see stale data that the caller needs to be fresh?" Supply key construction, set/delete code, DB write and readers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Global data; staleness explicitly acceptable.

## Examples
Flag:
```python
cache.set(f"profile:{user_id}", data, ex=300000)     # intended 5 min, ex is seconds
```
OK:
```python
cache.set(f"t:{tenant}:profile:{user_id}", data, ex=300)
```

## Evaluation cases
1. Above flag example -> flag.
2. `cache.delete(k)` then `db.commit()`; a reader repopulates between -> flag.
3. Key by tenant and user with `ex=300` -> no finding.
4. Per-user data keyed `report:latest` -> flag.
