# common/lost-error-detail: Unexpected failure logged without error object or stack trace

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | logging-review: What to check, Lost error detail |

## Intent
Logging only the message of an exception (or nothing from it) discards the stack trace and cause that diagnosis needs.

## Detect
In a catch/except/rescue block, a log call at warn level or above that references the caught variable only through its message (`e.getMessage()`, `e.message`, `str(e)`, `${e}` without the object as the final logger argument, `err.Error()` in Go with no wrapping), and does not pass the exception object, `exc_info=True`, `log.exception(...)` or equivalent.

## Do not flag
- Logging that passes the exception as the throwable/cause argument, or `log.exception`.
- Expected, handled conditions (validation, not-found) where the message is sufficient.
- A block that rethrows the original exception.
- Tests and throwaway scripts.

## Examples
Flag: `log.error("Failed: " + e.getMessage());`
OK: `log.error("Failed for {}", id, e);`

## Implementation notes
Anchor to the log call. Per-language table of "exception passed" forms. Always `INFO`; never costs score.

Java files only, for now. A warn or error log in a `catch` that passes the message or string form of the exception (`getMessage()`, `toString()`) and not the exception itself. Exception types whose name contains `NotFound`, `Validation`, `Invalid`, `Parse`, `NumberFormat`, `Timeout`, `Conflict` or `BadRequest` are treated as expected, and a catch that throws something carrying the original is skipped.

## Required tests
`getMessage()` only (candidate), exception passed (none), `log.exception` (none), `exc_info=True` (none), rethrow (none).

## AI prompt
"Is the failure caught here unexpected, such that diagnosing it would need the stack trace or cause? Or is it an expected, handled condition for which the message is enough?" Evidence: the catch block, the exception type, the try body.

## Why hybrid
Message-only logging is detectable; whether the condition is unexpected needs the surrounding intent.
