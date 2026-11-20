# common/log-injection: Untrusted input written verbatim to a log line

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING (SECURITY when the input is clearly attacker-controlled) |
| Severity | INFO (normal severity when reported as SECURITY) |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | logging-review: What to check, Log injection |

## Intent
Line breaks or control characters in user input can forge or hide log entries.

## Detect
A log call whose argument is a value read from an HTTP request (`getParameter`, `getHeader`, `request.args`, `req.query`, `req.body`, `@RequestParam`/`@PathVariable`/`@RequestBody` parameters, `r.FormValue`), or a variable assigned from one in the same function, with no sanitiser (`replaceAll("[\r\n]", ...)`, encoder helper, `%q`/JSON encoder, structured logger that escapes).

## Do not flag
- Values that are validated as numbers, enums or UUIDs before logging.
- JSON/structured loggers that escape control characters.
- Tests.

## Examples
Flag: `log.info("login " + request.getParameter("user"));`
OK: `log.info("login {}", sanitize(user));`

## Implementation notes
Intra-procedural taint from known request sources to log arguments. Anchor to the log call. Report as `SECURITY` only when the source is a request parameter, header or body; otherwise `LOGGING`/`INFO`.

## Required tests
Direct request parameter (finding), via local variable (finding), sanitised (none), integer parsed value (none), JSON logger (none).

## AI prompt
"Is the logged value attacker-controlled, and does the logging setup (layout, encoder) keep newlines intact? If clearly attacker-controlled and unescaped, report as SECURITY; if only possibly external, report as LOGGING/INFO." Evidence: the flow from source to log call, logging config if visible.

## Why hybrid
Source-to-sink flow is detectable; whether the log pipeline escapes control characters and how attacker-controlled the value is needs judgement.
