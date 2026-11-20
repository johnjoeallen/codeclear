# python/cancellation-deadline-propagation: Timeouts and cancellation do not stop work or undo writes

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Asyncio and background work; python-bug-hunter: Asyncio, generators, and exception ownership |

## Intent
`wait_for`, request deadlines or task cancellation interrupt only the awaiting coroutine. Thread/process work continues, a remote write may still commit, and cancellation during cleanup, lock release or commit can leave locks held or state half-written.

## Why AI
Needs the semantics of the awaited operation (thread offload, remote commit) and the cleanup/commit structure around the cancel point.

## AI prompt
"If this await is cancelled or times out at any point, are locks released, transactions rolled back or committed consistently, and is the caller prevented from assuming the underlying operation did not happen?" Supply the coroutine, its locks/transactions/cleanup, and what the awaited call does. Report only with a concrete trigger scenario (cancel point, resulting state, changed line); otherwise NO_FINDING.

## Do not flag
- Cleanup in `finally`/`async with` that is itself safe under cancellation.
- `shield` used with the task's completion observed.

## Examples
Flag:
```python
await asyncio.wait_for(loop.run_in_executor(None, write_file, data), 1)   # thread keeps writing
return "not written"
```
OK:
```python
async with lock:
    await asyncio.shield(commit(tx))
```

## Evaluation cases
1. Above flag example -> flag.
2. `wait_for(remote_charge())` then retry on `TimeoutError` -> flag (remote may have committed).
3. `async with` lock around an awaited read, cancelled -> no finding.
4. `await session.commit()` inside `except CancelledError` handler without shielding -> flag.
