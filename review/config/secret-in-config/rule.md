# config/secret-in-config: Credential committed in a configuration file

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | config-api-review: secrets checked into config |

## Intent
Secrets in source control are exposed to everyone with read access and persist in history.

## Detect
A key matching (case-insensitive) `(password|passwd|secret|token|api[-_.]?key|private[-_.]?key|client[-_.]?secret|credentials?)` whose value is a non-empty literal that is not a placeholder. Also values matching known formats: AWS access key `AKIA[0-9A-Z]{16}`, GitHub tokens `gh[pousr]_[A-Za-z0-9]{36,}`, `-----BEGIN .* PRIVATE KEY-----`, and URLs with embedded `user:password@`.

## Do not flag
- Placeholders: `${...}`, `{{...}}`, `<...>`, `changeme`, `xxx`, empty strings, `ENC(...)`, `vault:`, `secretsmanager:`.
- Keys that name a secret location instead of a value (`passwordFile`, `secretName`, `tokenUrl`).
- Files under `src/test/**` or named `*.example`, `*.sample`.

## Examples
Flag:
```yaml
spring.datasource.password: Pr0dPassw0rd!
```
OK:
```yaml
spring.datasource.password: ${DB_PASSWORD}
```

## Implementation notes
Parse YAML, properties and JSON into key paths so the key name is checked, not the whole line. Never print the secret value in the finding: print the key path and a masked hint only.

## Required tests
Literal password (finding), `${ENV}` (no finding), AWS key in a value (finding), `passwordFile` (no finding), test path (no finding), finding text does not contain the secret.

## Why deterministic
Key-name and value-format matching on parsed key paths is decidable; placeholders and test paths are explicit exclusions.
