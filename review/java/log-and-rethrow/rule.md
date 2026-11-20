# java/log-and-rethrow: Exception logged and rethrown (duplicate noise)

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | LOGGING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Exceptions |

## Intent
Logging an exception and then rethrowing it makes every layer log the same failure, creating duplicate noise and obscuring the root cause.

## Detect
A `catch` block that contains a log call at WARN/ERROR passing the caught exception (or its message) and then a `throw` of that exception or of a wrapper around it.

## Do not flag
- Log call that adds context not available upstream and the rethrown exception is not logged again by the boundary (the AI confirms).
- Boundary methods (controller advice, listeners) that log and translate.
- Test sources.

## Examples
Flag:
```java
catch (IOException e) { log.error("failed", e); throw e; }
```
OK:
```java
catch (IOException e) { throw new StorageException("failed to read " + path, e); }
```

## Implementation notes
AST. Anchor to the log call.

## Required tests
Log + rethrow (candidate), log + wrap (candidate), rethrow only (none), log only (none, see `catch-log-and-continue`).

## Why hybrid
Detection is syntactic; whether upstream already logs the failure is not visible in the file.

## AI prompt
"Does the caller or a boundary already log this exception, making this log duplicate noise or obscuring the root cause? Report only if so." Evidence: the catch block, the callers or boundary handler shown in the diff.
