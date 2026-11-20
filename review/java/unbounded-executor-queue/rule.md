# java/unbounded-executor-queue: Executor configuration allowing unbounded backlog or starvation

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | PERFORMANCE |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
Unbounded task queues or unbounded thread creation turn load spikes into memory exhaustion; a tiny shared pool starves independent work.

## Detect
- `Executors.newFixedThreadPool`, `newSingleThreadExecutor` (unbounded `LinkedBlockingQueue`) or `newCachedThreadPool` (unbounded threads) used for request or message work.
- `ThreadPoolTaskExecutor`/`ThreadPoolExecutor` with default `queueCapacity` (Integer.MAX_VALUE) or an unbounded queue and no rejection policy.
- `@Async` used with no custom executor bean defined in the shown context.

## Do not flag
- Bounded queue with explicit `RejectedExecutionHandler`.
- Test sources.

## Examples
Flag:
```java
ExecutorService pool = Executors.newFixedThreadPool(4); // fed per incoming message
```
OK: `new ThreadPoolExecutor(4, 4, 0, SECONDS, new ArrayBlockingQueue<>(1000), new CallerRunsPolicy())`.

## Implementation notes
AST/regex on executor factory calls. Anchor to the creation. Do not assume Spring defaults when configuration is absent from context.

## Left to the AI
Pools for a fixed, small set of known tasks (startup jobs): the detector cannot tell, so the AI confirms each candidate.

## Required tests
`newFixedThreadPool` (candidate), bounded `ThreadPoolExecutor` (none), `ThreadPoolTaskExecutor` with `setQueueCapacity(100)` (none).

## Why hybrid
Whether a backlog can grow without limit depends on the submit rate and on config outside the file.

## AI prompt
"Can the submit rate exceed the pool throughput so the queue/thread count grows unbounded, or can unrelated tasks starve each other? Report only with a concrete producer rate source and visible configuration." Evidence: executor setup, producers, application properties.
