# common/wrong-log-level: Log level does not match the severity of the event

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | LOW |
| Detection type | ai |
| Source | logging-review: What to check, Wrong level |

## Intent
Expected conditions logged as errors cause alert noise; real failures logged at debug/info are missed; per-item logging at info or above floods production logs.

## Why AI
Whether a condition is expected or needs attention depends on the application's semantics and operational conventions. Even the "per-loop" case depends on call frequency, which is not in the syntax.

## AI prompt
"For each added or changed log statement, judge the level against the event. Report (a) expected, handled conditions (validation failure, not-found, retry that will succeed) logged at error; (b) a real failure needing attention logged at debug/info; (c) a log at info or above inside a per-item or per-request loop that will flood production logs. Name the statement, the level, and why the event is of the other severity. Category LOGGING, severity INFO."
Evidence: the log statement, the enclosing handler or loop, how the caller handles the condition, level conventions used elsewhere in the file.

## Do not flag
- Wording, punctuation or message style; logging library or configuration not visible.
- Levels consistent with the file's existing conventions.
- Tests, examples, throwaway scripts.

## Examples
Flag: `log.error("User {} not found", id); return Optional.empty();`
OK: `log.debug("User {} not found", id); return Optional.empty();`

## Evaluation cases
1. `log.info` inside `for (item : items)` over a large batch: flag.
2. `log.error` on a 400 validation failure: flag.
3. `log.debug("DB write failed", e)` then rethrow-less fallback: flag.
4. `log.warn` for a retry that exhausted attempts: ok.
