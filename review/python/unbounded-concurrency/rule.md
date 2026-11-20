# python/unbounded-concurrency: Unbounded gather, task creation, queue or executor submission

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Asyncio and background work; Resources, deployment, and shutdown |

## Intent
Spawning one task/future per input item, or using an unbounded `asyncio.Queue`/`queue.Queue`, exhausts memory, connections or downstream capacity. A semaphore acquired only around task creation does not bound the work itself.

## Detect
Candidates:
- `asyncio.gather(*[...for x in xs])`, `create_task` or `executor.submit` inside a loop/comprehension over a collection not provably small;
- `Queue()`/`Queue(maxsize=0)` fed by an unbounded producer;
- `Semaphore` used outside the coroutine body that performs the I/O.

## Do not flag
- Iterables of literal or constant small size; semaphore acquired inside the task around the call.
- Bounded queues with backpressure.

## Examples
Flag:
```python
await asyncio.gather(*(fetch(u) for u in urls))    # urls from request
```
OK:
```python
async def bounded(u):
    async with sem: return await fetch(u)
await asyncio.gather(*(bounded(u) for u in urls))
```

## Implementation notes
Python `ast`; anchor to the spawning expression.

## AI prompt
Question: "Can the input size realistically exceed downstream/pool/memory capacity shown in the code or config?" Supply the source of the collection, pool and client limits. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Fan-out syntax is deterministic; the input size and capacity are contextual.
