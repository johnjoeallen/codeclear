# java/async-failure-unobserved: Async work whose failure nobody observes

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
Exceptions thrown in `@Async` methods, executor tasks and `CompletableFuture` stages vanish (or only reach a default handler) when nothing reads the result, so failures look like success.

## Detect
- An `@Async` method returning `void` with no `AsyncUncaughtExceptionHandler` or internal try/catch that records the failure.
- `executor.submit(...)`/`execute(...)`/`CompletableFuture.runAsync/supplyAsync` whose returned future is discarded and whose task body does not catch and report exceptions.
- A `CompletableFuture` chain without `exceptionally`, `handle` or `whenComplete`, whose result is never joined.

## Do not flag
- Futures that are returned, stored, joined or composed with an error handler.
- Tasks whose body wraps everything in a try/catch that logs or records.
- Configured `AsyncUncaughtExceptionHandler` shown in context.

## Examples
Flag:
```java
@Async public void sendEmail(Mail m) { client.send(m); }
```
OK: `sendEmail` returns `CompletableFuture<Void>` that the caller observes, or catches and records failures.

## Implementation notes
AST. Anchor to the method or submit call.

Two files: `async.dsl` flags an `@Async` method that returns `void` and has no `catch`, unless some file of the project mentions `AsyncUncaughtExceptionHandler`; `rule.dsl` flags a discarded `submit`/`execute` on an executor, a discarded `runAsync`/`supplyAsync`, and a discarded `then*` chain, unless the statement has a `catch` or `exceptionally`/`handle`/`whenComplete`/`join`/`get`. A stored or returned future is never discarded, so it is not flagged.

## Required tests
`@Async void` without handler (candidate), returned future (none), discarded `supplyAsync` (candidate), try/catch body (none).

## Why hybrid
A handler or observer may be configured elsewhere.

## AI prompt
"Do failures of this async work reach any observer (handler, joined future, retry, alert)? Report only if the failure is silently lost and describe the impact." Evidence: async method/task, caller, `AsyncConfigurer`, executor config.
