# common/misleading-log-message: Log text contradicts the code

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | logging-review: What to check, Misleading messages |

## Intent
A message that says "retrying" when nothing retries, "saved" before the write happens, or names the wrong entity or value, sends operators in the wrong direction.

## Why AI
Comparing prose to behaviour, and checking that an argument is the right entity, needs semantic understanding. Placeholder/argument count is handled by `common/log-placeholder-mismatch`.

## AI prompt
"For each added or changed log statement, compare its text and arguments with the code around it. Report if the text states something the code contradicts (retrying with no retry, success before the operation, wrong entity or variable, arguments in the wrong order for the placeholders). Quote the message and the contradicting line. Category LOGGING, severity INFO."
Evidence: the log statement and the enclosing function, variable types and names in scope.

## Do not flag
- Wording, punctuation, capitalisation or style preferences.
- Placeholder count errors (other rule).
- Tests, examples, throwaway scripts.

## Examples
Flag: `log.info("Order {} saved", id); repository.save(order);`
OK: `repository.save(order); log.info("Order {} saved", id);`

## Evaluation cases
1. `log.warn("Retrying {}", job)` followed by `throw`: flag.
2. `log.info("user {} deleted", orderId)`: flag.
3. `log.info("moved {} to {}", target, source)` where the code moves source to target: flag.
4. Informal wording `"oops, failed"`: ok.
