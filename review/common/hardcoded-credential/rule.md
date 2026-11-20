# common/hardcoded-credential: Credential or key in source code

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | java-review: Spring; python-review: Security; comment-quality-review |

## Intent
Credentials in code or comments are exposed to everyone with repository access, and persist in history.

## Detect
- Assignment or argument where the name matches `(password|passwd|pwd|secret|token|api[-_]?key|private[-_]?key)` and the value is a non-empty string literal of 8 or more characters that is not a placeholder.
- Known token formats: `AKIA[0-9A-Z]{16}`, `gh[pousr]_[A-Za-z0-9]{36,}`, `xox[baprs]-`, `-----BEGIN (RSA |EC )?PRIVATE KEY-----`, `eyJ[A-Za-z0-9_-]{10,}\.eyJ`.
- The same patterns inside comments.
- URLs with `user:password@host`.

## Do not flag
- Environment lookups, placeholders (`changeme`, `xxx`, `<token>`), empty strings, and constants that name a key (`PASSWORD_FIELD = "password"`).
- Test files using obviously fake values (`test`, `dummy`, `example`).

## Examples
Flag:
```java
private static final String API_KEY = "sk_live_9f8a7d6c5b4a";
```
OK:
```java
private final String apiKey = System.getenv("API_KEY");
```

## Implementation notes
Regex with an entropy check on the value (Shannon entropy above 3.0 for 16 or more characters) to cut false positives. Never print the value; mask all but the first 4 characters. Consider wrapping `gitleaks` or `detect-secrets`.

## Required tests
Hardcoded key (finding), env lookup (no finding), token inside a comment (finding), placeholder (no finding), masked output.

## Why deterministic
Known token formats and secret-named assignments with literal values are pattern matches. A credential found in a comment is reported as `SECURITY`, not `COMMENT_QUALITY`.
