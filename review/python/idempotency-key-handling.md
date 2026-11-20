# python/idempotency-key-handling: Idempotency reservation, payload conflict and replay defects

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Microservices and distributed side effects; python-bug-hunter: Distributed workflows and microservices |

## Intent
An endpoint promising deduplication must scope keys correctly (per tenant/endpoint), reserve atomically, reject a different payload under the same key, return the stored result, and not leave failed work recorded as completed or let reservation expiry reopen duplicates.

## Why AI
Requires the full reserve -> execute -> store-result protocol across storage, TTLs and concurrency.

## AI prompt
"Trace reservation, payload comparison, business commit and cached response. Under two concurrent requests with the same key, a crash after commit, a failure before commit, and TTL expiry, can the operation run twice or can a failed operation be replayed as success?" Supply the handler, key storage code, TTLs and constraints. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Atomic reservation via unique constraint/`SET NX` with status and payload hash checks; endpoints that make no idempotency claim.

## Examples
Flag:
```python
if redis.get(key): return json.loads(redis.get(key))
result = charge(req); redis.set(key, json.dumps(result), ex=60)     # check-then-set race, 60 s TTL
```
OK:
```python
if not redis.set(f"idem:{tenant}:{key}", "PENDING", nx=True, ex=86400): return replay_or_409(...)
```

## Evaluation cases
1. Above flag example -> flag.
2. Key stored as `COMPLETED` in a `finally` even when `charge` raised -> flag.
3. Unique-constraint reservation, payload hash compared, 409 on mismatch -> no finding.
4. Key not scoped per tenant while tenants can choose keys -> flag.
