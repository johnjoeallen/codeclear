# java/lock-not-released-in-finally: Lock acquired without unlock in finally

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Resources and I/O |

## Intent
If code between `lock()` and `unlock()` throws, the lock stays held and every later caller blocks forever.

## Detect
A call to `lock()`, `lockInterruptibly()`, `readLock().lock()`, `writeLock().lock()` or `Semaphore.acquire()` on a `Lock`/`Semaphore` expression where the matching `unlock()`/`release()` on the same expression is not in the `finally` of a `try` that begins immediately after the acquire.

## Do not flag
- `tryLock()` guarded by `if`, whose `unlock()` is in a `finally` inside the branch.
- An acquire inside the `try` block of a `try` whose `finally` releases the same lock (`try { lock.lock(); ... } finally { lock.unlock(); }`): the lock is released, although acquiring before the `try` is the safer form.
- Acquire/release split across methods of a lock-wrapper class (`acquire`/`release` pairs with the same field) marked `confidence: LOW`.

## Examples
Flag:
```java
lock.lock();
work();
lock.unlock();
```
OK:
```java
lock.lock();
try { work(); } finally { lock.unlock(); }
```

## Implementation notes
AST on the enclosing block. Anchor to the acquire call. Equivalent: SpotBugs `UL_UNRELEASED_LOCK`.

## Required tests
No try (finding), unlock outside finally (finding), correct pattern (none), `tryLock` with finally (none).

## Why deterministic
Lock acquire and release pairing within one method is a structural property of the syntax tree.
