# python/remote-call-inside-transaction: Network call, message send or sleep while a transaction is open

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Databases, ORM, and transactions |

## Intent
HTTP/gRPC calls, broker publishes or `sleep` inside an open transaction hold locks and pool connections for the remote latency, and their side effects cannot be rolled back.

## Detect
Candidates: inside `with transaction.atomic()`, `with session.begin()`, `with conn:`/`@transaction.atomic` function bodies, a call to `requests.*`, `httpx.*`, `aiohttp` session methods, `*.publish`, `producer.send/produce`, `boto3` client calls, `.delay()/.apply_async()`, or `time.sleep`/`asyncio.sleep`.

## Do not flag
- `transaction.on_commit(...)` registrations; calls after the block exits.
- Calls to the same database; read-only local helpers.

## Examples
Flag:
```python
with transaction.atomic():
    order.save()
    requests.post(PAY_URL, json=...)
```
OK:
```python
with transaction.atomic():
    order.save()
resp = requests.post(PAY_URL, json=...)
```

## Implementation notes
Python `ast` with a client-call blocklist; anchor to the call.

Detected on calls to `requests.*`, `httpx.*`, a `*session*`/`*client*`/`*http*` verb, `publish`, `produce`, `apply_async`, `delay`, a producer `send`, `time.sleep` and `asyncio.sleep`, inside a `with` whose header mentions `atomic`, `begin`, `conn`, `connection` or `transaction`, or inside a function decorated with `@...atomic`. A call inside an argument of `on_commit(...)` is skipped, since it runs after the commit. `boto3` client calls are not matched by name.

## AI prompt
Question: "Does this call inside the transaction cause lock/pool pressure or an irreversible side effect that rollback cannot undo, given the shown latency and failure behavior?" Supply the block and the called client. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Placement is syntactic; impact depends on locks held, latency and what the call does.
