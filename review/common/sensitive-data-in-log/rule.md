# common/sensitive-data-in-log: Secret or personal data written to a log

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | logging-review: exception for credentials and personal data |

## Intent
Logs are copied to many systems and kept for long periods. Credentials and personal data in them are a security defect.

## Detect
A log call (`log.*`, `logger.*`, `console.*`, `logging.*`, `slog.*`, `print`) where an argument or interpolation is, or is named after, `password`, `passwd`, `secret`, `token`, `apiKey`, `authorization`, `cookie`, `ssn`, `cardNumber`, `cvv`, `iban`, or where the argument is a whole request, headers or user object.

## Do not flag
- Messages that only mention the word in text (`"password reset requested"`) without a value.
- Values passed through a masking helper (`mask(...)`, `redact(...)`).
- Test files.

## Examples
Flag:
```java
log.info("login for {} with {}", user, password);
```
OK:
```java
log.info("login for {}", user.getId());
```

## Implementation notes
Detect the log call and the sensitive name in its arguments. Report as `SECURITY`, not `LOGGING`.

Java files only, for now. A log call (`log`/`logger` levels, `System.out.print*`) with an argument that uses a variable, field or getter named like a secret (`password`, `token`, `apiKey`, `authorization`, `cookie`, `ssn`, `cardNumber`, `cvv`, `iban`), or a whole `request`, `headers` or `user` object. Names are read from the code, never from string text, so "password reset requested" does not match. An argument containing a call named `mask`, `redact`, `obfuscate`, `hide` or `sanitize` is skipped.

## Required tests
Password argument (finding), f-string with `token` (finding), masked value (no finding), plain text mention (no finding).

## AI prompt
"Does the logged value actually contain a credential, token, key, full payment or identity number, or other personal data, unmasked? Report only if the variable demonstrably carries such data." Evidence: the log call, the variable's definition and type, any masking helper in scope.

## Why hybrid
Names and whole-object arguments are detectable, but whether the value really holds secret or personal data needs the data's origin.
