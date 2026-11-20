# java/exception-cause-dropped: Exception wrapped without its cause

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Exceptions; java-bug-hunter: Exceptions, resources and control flow |

## Intent
Throwing a new exception from a `catch` without passing the caught exception loses the root cause and stack trace.

## Detect
- A `throw new X(...)` inside a `catch` block where no argument references the catch parameter
  and `initCause` / `addSuppressed` with it is not called.

## Do not flag
- The cause is passed in any argument position, including `e.getCause()` or a local variable assigned from it.
- The new exception type has no constructor accepting a cause (checked when the class is in the same file or is a JDK class).
- The catch parameter is deliberately discarded and a comment says why.

## Examples
Flag:
```java
catch (SQLException e) { throw new RepositoryException("load failed"); }
```
OK:
```java
catch (SQLException e) { throw new RepositoryException("load failed", e); }
```

## Implementation notes
AST walk: for each `ThrowStmt` with an `ObjectCreationExpr` inside a `CatchClause`, check the
arguments for a `NameExpr` equal to the parameter. Anchor to the `throw` line.

## Required tests
Cause omitted (finding), cause passed (no finding), `e.getCause()` passed (no finding), nested
catch (inner parameter scoped correctly), throw outside a catch (no finding).
