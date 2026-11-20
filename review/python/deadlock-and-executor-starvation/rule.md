# python/deadlock-and-executor-starvation: Opposing lock order, unreleased locks or pool self-wait

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Threads, processes, and request context |

## Intent
Two paths acquire locks in opposite order, a lock is not released after an exception, or tasks running on a bounded executor wait for work submitted to the same executor, so all workers block.

## Why AI
Needs both lock acquisition paths or the pool size and the dependency between tasks, usually across functions or files.

## AI prompt
"Identify two concrete code paths that take lock A then B and B then A, a path where an exception skips release, or tasks that `.result()`/`await` work submitted to the same bounded pool. State the exact interleaving." Supply all lock sites, executor construction (max_workers) and submit/wait sites. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Locks taken via `with`; a consistent global order; waits on a different pool or unbounded pool.

## Examples
Flag:
```python
def f(): lock_a.acquire(); lock_b.acquire(); ...
def g(): with lock_b: with lock_a: ...
```
OK:
```python
def f(): with lock_a, lock_b: ...
def g(): with lock_a, lock_b: ...
```

## Evaluation cases
1. Above flag example -> flag.
2. `ThreadPoolExecutor(2)`; each task calls `pool.submit(sub).result()` -> flag.
3. `lock.acquire()` followed by code that may raise and `release()` not in `finally` -> flag.
4. Consistent order, `with` blocks -> no finding.
