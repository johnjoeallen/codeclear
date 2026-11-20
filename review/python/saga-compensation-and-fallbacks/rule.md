# python/saga-compensation-and-fallbacks: Compensation ordering/retry defects and fallbacks that mask failure

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Microservices and distributed side effects; python-bug-hunter: Distributed workflows and microservices |

## Intent
Shown saga compensations run in the wrong order, are not retried or idempotent, or fail silently; fallbacks return stale/default data as success, retry side effects, or bypass a security check.

## Why AI
Needs the workflow's steps and the business meaning of "stale" or "fallback" results; no pattern is mandatory without a concrete consistency need.

## AI prompt
"For each step's failure, does the compensation run in reverse order, tolerate retry/duplication, and surface its own failure? Does a fallback value cause false success, stale state, a retried side effect or an authorization bypass?" Supply the workflow, compensation code and fallback usage. Report only with a concrete trigger scenario; do not mandate sagas or circuit breakers; otherwise NO_FINDING.

## Do not flag
- Read-only fallbacks to cached data with accepted staleness; idempotent compensations that log and alert on failure.

## Examples
Flag:
```python
try: pay(); ship()
except Exception: refund()          # refund failure swallowed, ship may have partly happened
```
OK:
```python
except Exception:
    compensations.enqueue(refund, idempotency_key=order.id)
```

## Evaluation cases
1. Above flag example where `refund` raising is lost -> flag.
2. `except TimeoutError: return {"authorized": True}` as fallback -> flag.
3. Fallback to last cached price list flagged "may be 5 min stale" -> no finding.
4. Compensation retried through an idempotent queue -> no finding.
