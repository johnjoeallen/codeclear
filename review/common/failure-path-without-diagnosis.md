# common/failure-path-without-diagnosis: Error or fallback path logs nothing useful

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | LOW |
| Detection type | ai |
| Source | logging-review: What to check, Failure paths without useful diagnosis |

## Intent
An error or fallback path that logs nothing, or only a fixed text, leaves an operator unable to tell which entity, operation or request failed.

## Why AI
Deciding that a branch is a failure path, and which identifier would identify the failing item, needs the surrounding code and domain meaning. Presence of a log call is deterministic; sufficiency of its content is not.

## AI prompt
"For each added or changed catch block, error branch or fallback in this diff, does it log nothing, or log only a constant message with no identifier of the affected entity, operation or request that is available in scope? Name the identifier that is in scope and missing. Report as LOGGING / INFO. If the code also swallows the failure or reports false success, do not report here; that is an error-handling defect under its own category."
Evidence: the changed function in full, variables in scope, the logging style used in the file.

## Do not flag
- Absence of logging on simple, successful paths.
- Paths that rethrow with context or are logged by a caller shown in the supplied code.
- Tests, examples, throwaway scripts; wording, punctuation or style preferences; logging library choices not visible.

## Examples
Flag: `catch (IOException e) { log.error("Import failed"); }` where `fileId` is in scope.
OK: `log.error("Import failed for file {}", fileId, e);`

## Evaluation cases
1. Fallback to default value with no log, id in scope: flag.
2. `log.warn("payment failed")` in a loop over orders: flag.
3. Error path logs `orderId` and exception: ok.
4. Validation helper returning false on a successful path: ok.
