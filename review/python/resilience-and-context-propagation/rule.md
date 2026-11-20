# python/resilience-and-context-propagation: Circuit breaker, bulkhead, deadline budget and trace/auth propagation misuse

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Microservices and distributed side effects |

## Intent
Circuit-breaker fallbacks that report success, shared bulkheads that let one dependency starve others, deadline budgets not propagated to downstream calls, and dropped trace/auth headers cause demonstrated failure, exposure or false success.

## Why AI
Report only when absence or misuse leads to a concrete failure; needs the call topology and configuration.

## AI prompt
"Does the changed use or omission of a circuit breaker, bulkhead, timeout/deadline budget or trace/auth header propagation cause a demonstrable failure, privilege/identity loss, or false success?" Supply the client wrapper, configuration and callers. Report only with a concrete trigger scenario; absence of a pattern alone is not a finding; otherwise NO_FINDING.

## Do not flag
- Calls where the gateway/mesh handles propagation (shown); services with no downstream calls.

## Examples
Flag:
```python
requests.get(f"{SVC}/orders", headers={}, timeout=5)     # service requires user's Authorization; fallback masks 401
```
OK:
```python
requests.get(url, headers={"Authorization": req.headers["Authorization"], "traceparent": tp}, timeout=remaining)
```

## Evaluation cases
1. Outbound call to a user-scoped API using a service token, losing the user identity, where the downstream authorizes by token -> flag.
2. Inner timeout 30 s inside a request deadline of 5 s -> flag.
3. Missing circuit breaker on an internal call with no failure history shown -> no finding.
4. Breaker fallback returns `[]` and caller treats as "no orders" -> flag.
