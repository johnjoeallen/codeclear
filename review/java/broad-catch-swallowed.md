# java/broad-catch-swallowed: Broad catch that continues as if successful

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Exceptions; java-bug-hunter: Exceptions, resources and control flow |

## Intent
Catching `Exception`, `Throwable` or `RuntimeException` and then returning a success-looking value
hides errors from callers.

## Detect
A `catch` of `Exception`, `Throwable` or `RuntimeException` whose block neither rethrows nor
throws, and which either returns a literal (`true`, `null`, empty collection, `Optional.empty()`)
or falls through to code after the `try`.

## Do not flag
- Top-level boundaries: methods annotated `@ExceptionHandler`, `main`, `Runnable.run`, scheduled
  jobs and message listeners that map the failure to a response, status or dead-letter.
- A block that records the failure to a field or result object returned to the caller.
- `catch (Throwable t)` that rethrows `Error` subclasses.

## Examples
Flag:
```java
try { return client.fetch(id); } catch (Exception e) { return null; }
```
OK:
```java
try { return client.fetch(id); } catch (IOException e) { throw new FetchException(id, e); }
```

## Implementation notes
AST finds candidates. Because boundary detection is context-dependent, mark `hybrid`: the AI
reviewer confirms whether the catch sits at a legitimate boundary.

## AI prompt
For each candidate `catch`, using the enclosing method and its callers if supplied: is this a legitimate boundary that classifies and surfaces the failure, or does the caller see success after a failure? Report only with a concrete scenario ("when X fails, the caller receives Y because of line N"). Evidence: the method, its callers, any `@ExceptionHandler` or listener configuration.

## Why hybrid
Catching `Exception` is syntactic, but whether the site is a valid boundary depends on callers.

## Required tests
Return null (finding), rethrow (no finding), `@ExceptionHandler` method (no finding), specific
exception type (no finding).
