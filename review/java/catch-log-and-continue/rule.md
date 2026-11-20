# java/catch-log-and-continue: Catch that only logs when the caller needs the failure

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Exceptions |

## Intent
A `catch` that logs at WARN/ERROR and then continues makes the method report success although the operation failed.

## Detect
A `catch` block whose only statements are logging calls (INFO or higher, `printStackTrace`) with no `throw`, no `return` of an error value, no assignment to a failure field, in a method that returns `void` or a value the caller uses.

## Do not flag
- Cleanup in `finally`-style best-effort code (`close`, `delete`) where failure is irrelevant.
- Top-level boundaries (`main`, `run`, listeners, `@ExceptionHandler`) that map the failure elsewhere.
- Test sources.

## Examples
Flag:
```java
try { repo.save(order); } catch (DataAccessException e) { log.error("save failed", e); }
return Result.ok();
```
OK:
```java
try { repo.save(order); } catch (DataAccessException e) { throw new OrderSaveException(order.id(), e); }
```

## Implementation notes
AST on `catch` blocks (JavaParser). Anchor to the `catch` line. Empty and DEBUG-only blocks belong to `empty-catch-block`.

## Required tests
Log-only catch in non-void method (candidate), log then rethrow (none), `close()` cleanup (none), test path (none).

## Why hybrid
The syntax is deterministic, but whether the caller needs to know depends on the call site and method contract.

## AI prompt
"Given this catch block and the method containing it, does a caller depend on the operation having succeeded? Reply `report` only if you can state a concrete scenario where a failure is silently treated as success." Evidence: the method body, its signature, and the callers shown in the diff.
