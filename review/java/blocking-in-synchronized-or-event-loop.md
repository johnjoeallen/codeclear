# java/blocking-in-synchronized-or-event-loop: Blocking call under a lock or on a non-blocking thread

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Concurrency; java-bug-hunter: Concurrency and asynchronous work |

## Intent
Blocking I/O, sleeps or future waits inside `synchronized` blocks serialise all threads behind a slow call; on reactive/event-loop or common-pool threads they starve the whole runtime.

## Detect
- A `synchronized` block/method or held `Lock` region containing `Thread.sleep`, HTTP/JDBC/file calls, `Future.get`, `CompletableFuture.join/get`, `BlockingQueue.take`.
- In `Mono`/`Flux`/`WebFlux` operators, Netty handlers, or `CompletableFuture.supplyAsync(...)` without a custom executor, the same blocking calls, `block()` or `.join()`.

## Do not flag
- Blocking on a dedicated executor (`subscribeOn(Schedulers.boundedElastic())`, custom pool).
- Test sources.

## Examples
Flag:
```java
synchronized (lock) { var r = client.get(url); cache.put(id, r); }
```
OK: fetch outside the lock, then lock only to update `cache`.

## Implementation notes
AST on `synchronized` and lambda bodies passed to reactive operators. Anchor to the blocking call.

## Required tests
HTTP in synchronized (candidate), `block()` inside `map` (candidate), `boundedElastic` (none), no blocking (none).

## Why hybrid
Which calls are blocking and how long they take depends on the callee and threading context.

## AI prompt
"Does this call block while a lock is held or on a thread that must not block, and what contention or starvation results?" Evidence: the block, callee signature, executor/scheduler context.
