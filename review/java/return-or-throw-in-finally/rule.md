# java/return-or-throw-in-finally: Return or throw inside finally

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Exceptions |

## Intent
A `return`, `break`, `continue` that leaves the block, or `throw` in `finally` discards any exception
propagating from the `try` or `catch`, and can replace a returned value.

## Detect
A `ReturnStmt` or `ThrowStmt` that is directly in a `finally` block (not inside a nested lambda,
anonymous class or nested method). Also `break`/`continue` whose target is outside the `finally`.

## Do not flag
- A `throw` inside a nested `try` within the `finally` that is fully caught by that nested `try`.
- Returns inside a lambda declared in the `finally`.

## Examples
Flag:
```java
try { work(); } finally { return result; }
```
OK:
```java
try { work(); } finally { cleanup(); }
```

## Implementation notes
AST. Anchor to the `return`/`throw` line. Equivalent: Checkstyle `ReturnFromFinally`, Sonar `S1143`.

## Required tests
Return in finally, throw in finally, nested caught throw (no finding), lambda return (no finding).
