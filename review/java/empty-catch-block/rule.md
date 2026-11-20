# java/empty-catch-block: Empty catch block

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Exceptions |

## Intent
A `catch` that does nothing hides the failure, so the caller continues as if the operation worked.

## Detect
- A `catch` clause whose block has no statements and no comments.
- A `catch` block containing only a log call at DEBUG/TRACE level, with no rethrow, return or fallback value.

## Do not flag
- A block with a comment explaining why the exception is ignored.
- Catch parameter named `ignored`, `ignore` or `expected`.
- Catching `InterruptedException` that restores the flag (covered by `interrupt-flag-not-restored`).
- Test sources (`src/test/**`).

## Examples
Flag:
```java
try { files.delete(path); } catch (IOException e) { }
```
OK:
```java
try { files.delete(path); } catch (IOException ignored) { /* best effort cleanup */ }
```

## Implementation notes
Parse with JavaParser or the compiler tree API. Anchor to the `catch` line.
Equivalent: Checkstyle `EmptyCatchBlock`, PMD `EmptyCatchBlock`.

## Required tests
Empty block, block with only whitespace, block with comment (no finding), `ignored` parameter
(no finding), debug-only log (finding), test file path (no finding).
