# python/cache-nonatomic-read-modify-write: Non-atomic cache updates and separate value/expiry commands

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Caches and distributed locks |

## Intent
`GET` then `SET` on the same key loses concurrent updates; `SET` followed by `EXPIRE` leaves a key without TTL if the process dies between them or the second command fails.

## Detect
Candidates in one function:
- `r.get(k)` followed by `r.set(k, ...)` (or `incr`-like arithmetic) with no `WATCH`/pipeline `transaction=True`/Lua script/`INCR`;
- `r.set(k, v)` followed by `r.expire(k, ...)` instead of `set(..., ex=)`/`setex`.

## Do not flag
- `incr`, `hincrby`, `SET ... NX EX`, pipelines with `transaction=True` or Lua scripts; single-writer keys (shown).

## Examples
Flag:
```python
n = int(r.get(k) or 0); r.set(k, n + 1)
```
OK:
```python
r.incr(k)
```

## Implementation notes
Python `ast` call-sequence match on receiver names bound to `redis.Redis`/`StrictRedis`; anchor to the second call.

## AI prompt
Question: "Can concurrent callers interleave between the two commands with a harmful result, or can failure between them leave a permanent key?" Supply the code, concurrency and script/transaction use. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The sequence is deterministic; whether concurrency or failure matters depends on usage.
