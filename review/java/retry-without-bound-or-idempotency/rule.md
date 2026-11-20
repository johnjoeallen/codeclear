# java/retry-without-bound-or-idempotency: Retry loop unbounded, unclassified or on non-idempotent work

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Exceptions, resources and control flow; java-bug-hunter: AWS SDK; java-review: Backend boundaries and contracts |

## Intent
Retries without a limit and backoff hammer a failing dependency; retrying non-retryable errors is pointless; retrying a non-idempotent write after a partial side effect duplicates work.

## Detect
- A `while (true)`/`for(;;)`/recursive retry containing a catch that continues, with no attempt counter, deadline or break.
- A retry loop with no `sleep`/backoff between attempts.
- `@Retryable` with no `maxAttempts`/`backoff`, or `include`/`retryFor` of `Exception`/`Throwable`.
- A retry around a call to a write (`post`, `save`, `send`, `charge`, `putItem` without a condition).

## Do not flag
- Loops bounded by a counter or `Instant` deadline with backoff.
- Retries around reads or operations carrying an idempotency key.
- SDK retry policies explicitly configured with bounded attempts.

## Examples
Flag:
```java
while (true) { try { return http.post(url, body); } catch (IOException e) { } }
```
OK: three attempts with exponential backoff and an `Idempotency-Key` header.

## Implementation notes
AST for loops and annotations. Anchor to the loop or annotation.

## Required tests
Unbounded loop (candidate), bounded with backoff (none), `@Retryable` without limits (candidate), retry on read (none).

## Why hybrid
Idempotency and retryability of the specific failure type require callee knowledge.

## AI prompt
"Is this retry bounded and backed off, limited to retryable failures, and safe to repeat after a partial side effect? Report only with the failure type and the duplicated or unbounded effect." Evidence: retry code, callee semantics, idempotency keys, SDK/retry config.
