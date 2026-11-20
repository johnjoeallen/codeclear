# java/aws-async-future-blocked: Async AWS SDK future immediately blocked on

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: AWS SDK |

## Intent
Calling `.join()`/`.get()` on an AWS SDK v2 async client future turns it into a blocking call and, on an event-loop or Netty thread, can deadlock or stall the runtime.

## Detect
A method call on an `*AsyncClient` (e.g. `S3AsyncClient`, `DynamoDbAsyncClient`, `SqsAsyncClient`) whose returned `CompletableFuture` is immediately followed by `.join()` or `.get()`, or whose chained call ends in `.join()`.

## Do not flag
- Blocking at an application boundary (`main`, startup, a test) or on a dedicated blocking executor.
- `.join()` inside a `whenComplete` on an already completed future.

## Examples
Flag:
```java
var obj = s3Async.getObject(req, AsyncResponseTransformer.toBytes()).join();
```
OK: return the `CompletableFuture` and compose with `thenApply`, or use the sync client in blocking code.

## Implementation notes
AST with type resolution on `*AsyncClient`. Anchor to the `join`/`get` call.

## Required tests
Async client + `join()` (candidate), sync client (none), returned future (none), test source (none).

## Why hybrid
The defect needs knowledge that the calling thread is an event-loop or constrained thread.

## AI prompt
"Which thread executes this blocking wait? Report only if it is an event-loop/reactive/SDK callback thread or a bounded pool, naming the stall or deadlock." Evidence: method, its callers and executor context.
