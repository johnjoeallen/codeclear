# java/log-message-without-stacktrace: Unexpected failure logged without stack trace

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | LOGGING |
| Severity | LOW |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Exceptions |

## Intent
Logging only `e.getMessage()` or `e.toString()` drops the stack trace, so an unexpected failure cannot be diagnosed.

## Detect
In a `catch` of `Exception`, `RuntimeException`, `Throwable` or an unchecked type, a logger call at WARN/ERROR whose arguments contain `e.getMessage()` or `e.toString()` (or string concatenation of them) and which does not pass `e` itself as the last argument.

## Do not flag
- Catches of expected, specific checked exceptions (`NumberFormatException`, `ParseException`, `TimeoutException`) in validation code.
- Calls that also pass `e` as the throwable argument.
- Test sources.

## Examples
Flag:
```java
catch (Exception e) { log.error("failed: " + e.getMessage()); }
```
OK:
```java
catch (Exception e) { log.error("failed: {}", id, e); }
```

## Implementation notes
Match SLF4J/Log4j/JUL/commons-logging calls on a logger field. Anchor to the log call.

## Required tests
`getMessage()` only (finding), message plus `e` (none), `NumberFormatException` (none), `System.out.println(e.getMessage())` (none).

## Why deterministic
The catch type, log level and argument list are all visible in the syntax tree; the exclusion list is closed.
