# python/swallowed-exception-false-success: Caught exception reported as success

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Exceptions and cancellation; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
An `except` path logs and continues, returns a success value, or `gather(return_exceptions=True)` results are not inspected, so a failed operation is reported or committed as successful.

## Why AI
Whether recovery is safe depends on the caller's contract, what was partially done, and whether a boundary handler classifies the failure later.

## AI prompt
"Trace each changed `except`, `return_exceptions=True` result and suppressing context manager. After the failure, does the caller observe success (HTTP 200, task done, ack/commit, event published) although the required work did not complete?" Supply the function, its callers up to the boundary, and the transaction/ack handling. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Broad catch at a top-level boundary that classifies and surfaces failure.
- Best-effort side effects (metrics, cache warm) documented as optional.

## Examples
Flag:
```python
try:
    charge(order)
except Exception:
    log.exception("charge failed")
return {"status": "paid"}
```
OK:
```python
try:
    charge(order)
except PaymentError:
    log.exception("charge failed")
    raise
```

## Evaluation cases
1. Above flag example -> flag.
2. `results = await gather(*t, return_exceptions=True)` then only `len(results)` reported -> flag.
3. `except CacheError: pass` around cache warm-up -> no finding.
4. Handler catches `Exception`, maps to 502 and re-raises for nack -> no finding.
