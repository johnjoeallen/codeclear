# java/hardcoded-secret-or-environment-url: Secret or environment-specific URL in code

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Spring |

## Intent
Credentials and environment-specific endpoints hard-coded in source leak secrets and break deployment across environments.

## Detect
- A string literal assigned to or passed as a variable/parameter whose name matches `(?i)(password|passwd|secret|token|api[-_]?key|private[-_]?key|credential)` and that is non-empty and not a placeholder (`${...}`).
- A literal matching AWS access key `AKIA[0-9A-Z]{16}`, a PEM header, or `Bearer <token>`.
- A literal URL with `localhost`, an IP address, or hostnames containing `dev`, `staging`, `test`, `prod`, `internal` outside configuration classes.

## Do not flag
- Test sources, `@Value("${...}")` placeholders, empty strings, property-key names, schema/namespace URLs (`http://www.w3.org/...`).
- Obvious dummy values (`changeme` in a documented sample).

## Examples
Flag:
```java
private static final String API_KEY = "sk_live_9a8b7c6d";
```
OK:
```java
@Value("${payments.api-key}") private String apiKey;
```

## Implementation notes
Regex/AST for candidates. Anchor to the literal. Never echo the secret in the finding.

## Required tests
Hard-coded password (candidate), AKIA key (candidate), `@Value` placeholder (none), test file (none), localhost URL (candidate).

## Why hybrid
Placeholders, test fixtures and public URLs look identical to real secrets syntactically.

## AI prompt
"Is this literal a real secret or an environment-specific endpoint that should come from configuration?" Evidence: the literal's name, usage, surrounding file path (test/main) and project config conventions.
