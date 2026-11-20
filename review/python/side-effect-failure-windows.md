# python/side-effect-failure-windows: Success boundary and per-side-effect failure, duplicate and concurrency windows

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-bug-hunter: Trace the execution before judging it |

## Intent
Required work or cleanup does not finish at the intended success boundary (handler return, response iteration, commit, task result, message ack), or a side effect is unsafe when interrupted before/after, cancelled, invoked twice, or run concurrently.

## Why AI
Needs the execution owner and boundary definition and a supported scenario; the specific side-effect semantics vary.

## AI prompt
"Identify the success boundary of this flow. For each side effect, consider failure immediately before and after it, cancellation, duplicate invocation and two concurrent invocations, using only scenarios supported by the supplied context. Which produces an incorrect outcome?" Supply the flow's entry point, callers and consumers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Idempotent side effects; windows closed by shown mechanisms (transactions, unique constraints, acks after commit).

## Examples
Flag:
```python
send_email(user)           # side effect
user.mark_welcomed(); db.commit()      # crash between -> duplicate email on retry
```
OK:
```python
db.commit(); send_email(user)  # with dedup key on the email provider
```

## Evaluation cases
1. Above flag example where the task is retried by the broker -> flag.
2. Handler returns 200 before the awaited persistence finishes (task detached) -> flag.
3. Idempotent upsert repeated on retry -> no finding.
4. Concurrent duplicate guarded by unique constraint shown -> no finding.
