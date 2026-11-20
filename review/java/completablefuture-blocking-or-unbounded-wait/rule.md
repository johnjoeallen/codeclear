# java/completablefuture-blocking-or-unbounded-wait: CompletableFuture misuse (indefinite or blocking waits)

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
`join()`/`get()` without timeout waits forever if the stage never completes; blocking inside stages on the common pool or an event loop starves other work.

## Detect
- `CompletableFuture.get()`/`join()` with no timeout (and no prior `orTimeout`/`completeOnTimeout`).
- `supplyAsync`/`thenApplyAsync` without an executor argument whose lambda contains blocking I/O, `Thread.sleep`, `get()` or `join()`.
- `thenCompose` lambdas that call `join()` on another future.

## Do not flag
- Futures already known to be complete (for example after `allOf(...).join()` has returned).
- Calls with a bounded `get(timeout, unit)`.
- Test sources.

## Examples
Flag:
```java
var r = CompletableFuture.supplyAsync(() -> client.fetch(id)).join();
```
OK: `.orTimeout(2, SECONDS).join()` with a dedicated executor passed to `supplyAsync`.

## Implementation notes
AST. Anchor to the `join`/`get` or the lambda.

Detected by the receiver's declared type or a `CompletableFuture.supplyAsync(...)` chain. A future known to be complete (after `allOf(...).join()`) is not tracked: left to the AI.

## Required tests
`join()` no timeout (candidate), `orTimeout().join()` (none), blocking in `supplyAsync` default pool (candidate), custom executor (none).

## Why hybrid
Whether the stage can hang or the thread is constrained depends on execution context.

## AI prompt
"Can this future fail to complete or block a constrained thread in the execution context shown? Report only with the trigger." Evidence: chain, executor/pool, callee blocking behaviour.
