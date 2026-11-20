# common/duplicate-logging: Same failure logged at several layers

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | logging-review: What to check, Duplicate logging |

## Intent
Logging an exception and then rethrowing it makes every layer log the same event, producing repeated error entries for one failure.

## Detect
A catch/except block in changed code that contains both a log call at warn level or above using the caught exception, and a `throw`/`raise` of that same exception or a wrapper that has it as cause.

## Do not flag
- Blocks that log and then handle the failure without rethrowing.
- A boundary layer (controller advice, top-level handler, message listener) that is the designated single logging point.
- Logging at debug/trace, or without the exception.
- Tests.

## Examples
Flag:
```java
catch (SQLException e) { log.error("query failed", e); throw new DataException(e); }
```
OK:
```java
catch (SQLException e) { throw new DataException("query failed for " + id, e); }
```

## Implementation notes
AST: catch clause with a log call using the catch parameter plus a throw using it. Anchor to the log call.

## Required tests
Log then rethrow (candidate), log then return (none), throw with cause only (none), debug log then rethrow (none).

## AI prompt
"Is the rethrown exception logged again by a caller or boundary handler shown in the supplied code? Report only if the same failure would be logged at error level at least twice." Evidence: the catch block and its callers up to the boundary handler.

## Why hybrid
The log-and-rethrow shape is structural; duplication only happens if a caller logs it too, which needs cross-function context.
