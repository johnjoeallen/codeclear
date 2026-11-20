# common/unstructured-log-context: Key facts in free text where structured fields are used

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | LOW |
| Detection type | ai |
| Source | logging-review: What to check, Unstructured context |

## Intent
When the surrounding code logs with structured fields or a correlation/request id, a new entry that concatenates facts into text cannot be queried or correlated.

## Why AI
It depends on the logging convention of this file and project (MDC, `slog` attributes, `extra=`, structured logger wrappers), which must be inferred from other code, and on which facts matter.

## AI prompt
"First establish from the supplied file whether neighbouring log statements use structured fields or a correlation/request id. If so, report a new or changed log statement that puts identifiers into free text without those fields or omits the correlation id. Name the convention (with a line that shows it) and the statement. If the file shows no structured convention, report nothing. Category LOGGING, severity INFO."
Evidence: the changed log statements plus the rest of the file's logging calls, and logging config or helper if visible.

## Do not flag
- Files with no structured convention.
- Message style or wording preferences; logging library choices not visible.
- Tests, examples, throwaway scripts.

## Examples
Flag: `log.info("order " + id + " shipped to " + region)` in a file where other calls use `log.atInfo().addKeyValue("orderId", id)`.
OK: `log.atInfo().addKeyValue("orderId", id).log("order shipped")`.

## Evaluation cases
1. New `logger.info(f"user {uid} paid")` in a file using `extra={"user_id": uid}`: flag.
2. Same f-string in a file with only plain-text logs: ok.
3. Handler that skips the request id used everywhere else: flag.
