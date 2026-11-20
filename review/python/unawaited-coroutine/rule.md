# python/unawaited-coroutine: Coroutine called but not awaited or scheduled

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Asyncio and background work |

## Intent
Calling an `async def` function without `await` creates a coroutine that never runs. Work and exceptions are silently lost.

## Detect
An expression statement that is a call to a function defined `async def` in the same module, or
to `asyncio.sleep`, `asyncio.gather`, `asyncio.wait_for`, with no `await`, and not passed to
`asyncio.create_task`, `ensure_future`, `TaskGroup.create_task`, or returned.
Also flag `asyncio.create_task(...)` as an expression statement with the result discarded.

## Do not flag
- Calls inside a sync function that returns the coroutine to an awaiting caller.
- A stored task reference (`task = asyncio.create_task(...)`).

## Examples
Flag:
```python
async def run():
    save_user(user)       # save_user is async def
```
OK:
```python
async def run():
    await save_user(user)
```

## Implementation notes
Resolve only same-module `async def` names; cross-module resolution needs the AI reviewer (`hybrid`).
Equivalent: Ruff `RUF006` for discarded tasks, pyflakes-style `RuntimeWarning: coroutine was never awaited`.

## AI prompt
Question: "Is this call's result a coroutine that is neither awaited, scheduled, stored nor returned, so its work and exceptions are lost?" Supply the call, the callee definition (cross-module if needed) and the enclosing function. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Same-module `async def` callees are decidable syntactically; cross-module callees and callback wrappers need the AI to confirm.

## Required tests
Bare call (finding), awaited (no finding), `create_task` result discarded (finding), stored task (no finding).
