# python/sensitive-data-in-logs-and-errors: Secrets or personal data written to logs, errors or responses

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security |

## Intent
Passwords, tokens, keys, full request bodies/headers or personal data are logged or returned in error responses (stack traces, `str(e)`, `debug=True`).

## Detect
Candidates:
- logging calls (`logger.*`, `logging.*`, `print`) whose arguments include variables/attributes named `password`, `passwd`, `secret`, `token`, `authorization`, `api_key`, `ssn`, `card`, or whole `request.headers`/`request.body`/`request.json`;
- exception handlers returning `str(e)`/`traceback.format_exc()` in a response body;
- `app.run(debug=True)`/`DEBUG = True` in non-test settings.

## Do not flag
- Masked/redacted values (`"***"`, `mask(x)`), logging the presence or length only, test code, names that are not sensitive in context (`token_count`).

## Examples
Flag:
```python
log.info("login %s %s", user, password)
```
OK:
```python
log.info("login user=%s", user)
```

## Implementation notes
Python `ast` on logging calls with identifier-name matching; anchor to the call.

Detected on log calls (`logger`, `logging`, `log`, `print`) with an identifier named like a secret (`password`, `token`, `authorization`, `api_key`, `ssn`, `card_number`, `cvv`, ...) or a whole `request.headers/body/json/data/form/cookies`; on a `return` in an `except` block that contains `str(e)`, `repr(e)` or `traceback.format_exc()`; on `DEBUG = True` outside a dev/local/test file; and on `run(debug=True)`. A name ending in `count`, `length`, `type`, `url`, `file`, `path` or `name` is not sensitive, and an argument containing `mask(...)`, `redact(...)` and the like is skipped.

## AI prompt
Question: "Does this value contain a secret or personal data in practice, and is it redacted by a visible logging filter?" Supply the call, data definition and logging configuration. Report only with concrete exposure; otherwise NO_FINDING.

## Why hybrid
Name matching finds candidates; actual sensitivity and redaction filters need context.
