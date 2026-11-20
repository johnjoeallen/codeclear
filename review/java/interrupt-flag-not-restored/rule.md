# java/interrupt-flag-not-restored: InterruptedException swallowed

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Exceptions; java-bug-hunter: Exceptions, resources and control flow |

## Intent
Catching `InterruptedException` clears the thread's interrupt status. If the code neither
rethrows nor restores it, shutdown and cancellation requests are lost.

## Detect
A `catch` of `InterruptedException` (or a multi-catch including it) whose block contains none of:
- `Thread.currentThread().interrupt()`
- a `throw` statement

A bare `return` is not sufficient on its own.

## Do not flag
- The block rethrows `InterruptedException` or wraps it in another exception with the cause.
- The method is `main` or a test.
- The block only records the interruption (for example `interrupted = true`) and the method calls `Thread.currentThread().interrupt()` after the catch.

## Examples
Flag:
```java
try { queue.take(); } catch (InterruptedException e) { log.warn("interrupted"); }
```
OK:
```java
try { queue.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
```

## Implementation notes
AST. Anchor to the `catch` line. Equivalent: Sonar `S2142`, ErrorProne `InterruptedExceptionSwallowed`.

## Required tests
Log only (finding), restore flag (no finding), rethrow (no finding), multi-catch (finding),
exception wrapped with cause (no finding).
