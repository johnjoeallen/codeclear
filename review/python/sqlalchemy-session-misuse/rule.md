# python/sqlalchemy-session-misuse: Shared, reused-after-failure or escaping SQLAlchemy sessions

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Database invariants and transaction execution |

## Intent
`Session`/`AsyncSession` objects are stateful and not concurrency-safe: sharing across threads/tasks, reusing after a failed flush without `rollback()`, confusing flush with commit, or issuing queries through another session/engine escape the intended transaction.

## Why AI
Needs session ownership across functions, concurrency model, and which engine/session each query uses.

## AI prompt
"Is one session used concurrently (threads, `asyncio.gather`), reused after an exception without rollback, relied on after only `flush()`, or are queries issued on a different session/engine than the transaction?" Supply session creation, passing and every use. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- One session per task/request; `async with session.begin()`; deliberate separate-session audit writes.

## Examples
Flag:
```python
await asyncio.gather(*(repo.save(session, x) for x in xs))   # same AsyncSession
```
OK:
```python
async def save(x):
    async with Session() as s, s.begin(): ...
```

## Evaluation cases
1. Above flag example -> flag.
2. `session.flush()` raises, handler logs and then calls `session.add(...)`; `commit()` -> flag.
3. `session.flush()` followed by publishing an event with the generated id before commit -> flag if consumers read the DB.
4. Session created per request via dependency and closed in teardown -> no finding.
