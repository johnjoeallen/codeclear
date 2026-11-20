# python/retry-bounds-and-idempotency: Unbounded or non-idempotent retries after uncertain outcomes

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Exceptions and cancellation; Microservices and distributed side effects; python-bug-hunter: Distributed workflows and microservices |

## Intent
Retries without bounds, backoff or failure classification, retries that repeat a write which may already have committed (timeout after the remote succeeded), and nested retry layers with multiplied budgets or mismatched timeout units.

## Why AI
Whether the operation is idempotent, whether the remote committed, which errors are retryable, and the budgets of outer layers (client, framework, broker) are cross-file and system context.

## AI prompt
"Does a changed retry/timeout mechanism retry non-idempotent work after an outcome that may have committed, retry forever or without backoff, retry permanent errors, or stack with another retry layer so the total attempts/time exceed the caller's deadline?" Supply the retry code, the operation's idempotency (keys, upserts), client timeout settings with units, and outer retry configuration. Report only with a concrete trigger scenario (failure point, duplicate effect, changed line); otherwise NO_FINDING.

## Do not flag
- Retries of pure reads or operations shown idempotent (idempotency key, upsert, conditional write).
- Libraries (tenacity, urllib3 Retry) with bounded attempts and exponential backoff on retryable codes.

## Examples
Flag:
```python
for _ in range(5):
    try: return requests.post(url, json=payment, timeout=2)
    except requests.Timeout: continue    # payment may already be committed
```
OK:
```python
requests.post(url, json=payment, headers={"Idempotency-Key": key}, timeout=2)
```

## Evaluation cases
1. Above flag example -> flag.
2. `while True: try: call() except Exception: sleep(1)` with no limit -> flag.
3. Retry of `GET` with `Retry(total=3, backoff_factor=0.5)` -> no finding.
4. Client timeout `30` (ms library, 30 ms intended seconds) under a 3x tenacity wrapper over a 10 s gateway timeout -> flag.
