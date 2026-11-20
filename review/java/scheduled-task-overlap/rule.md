# java/scheduled-task-overlap: Scheduled tasks that block or overlap

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Spring and distributed-service behavior |

## Intent
Spring's default scheduler runs all `@Scheduled` tasks on one thread, so a long task delays the others. `fixedRate` tasks and multi-instance deployments can overlap or duplicate the same job.

## Detect
- `@Scheduled(fixedRate = ...)` on a method containing remote calls, loops over unbounded data or `sleep`, with no lock (ShedLock, `@SchedulerLock`) or in-flight guard.
- Multiple `@Scheduled` methods in the application with no `TaskScheduler`/`spring.task.scheduling.pool.size` configured (in context).
- A scheduled job that mutates shared data with no distributed lock where multiple instances are shown to run.

## Do not flag
- `fixedDelay` tasks with short bounded work.
- Jobs guarded by `@SchedulerLock` or idempotent by design.
- Single-instance deployments stated in context.

## Examples
Flag:
```java
@Scheduled(fixedRate = 1000) void sync() { client.pullAll(); }
```
OK: `@Scheduled(fixedDelay = 60000) @SchedulerLock(name = "sync")`.

## Implementation notes
AST on `@Scheduled`. Anchor to the annotation.

## Required tests
`fixedRate` with remote call and no lock (candidate), `fixedDelay` + `@SchedulerLock` (none), short in-memory task (none).

## Why hybrid
Overlap and starvation depend on runtime duration, scheduler pool and instance count.

## AI prompt
"Can this job run longer than its period, block other scheduled tasks on the shared scheduler, or run concurrently on several instances with a harmful effect? Report only with a concrete scenario using the shown configuration." Evidence: the job, scheduler config, deployment replicas, locking.
