# typescript/hardcoded-secret: Secret committed in source or config

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx`, `**/*.json` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | typescript-review: Security; node-review: AWS and serverless; node-review: Security |

## Intent
Credentials in source control are exposed to everyone with repository access and persist in history.

## Detect
- Known secret formats: `AKIA[0-9A-Z]{16}`, `-----BEGIN (RSA |EC )?PRIVATE KEY-----`, `ghp_[A-Za-z0-9]{36}`, `xox[abp]-`, `sk_live_[A-Za-z0-9]+`, JWT `eyJ...\.eyJ...\.`.
- Assignment of a non-empty string literal of length >= 12 to a name matching `password|passwd|secret|api[-_]?key|token|private[-_]?key|client[-_]?secret`, where the literal has high entropy or is not a placeholder.

## Do not flag
- Placeholders (`changeme`, `<token>`, `xxx`, `process.env.*`), test fixtures with obviously fake values, public keys, type names and enum values.
- Test files with the literal `test`/`example`/`dummy` in the value.

## Examples
Flag:
```ts
const apiKey = 'sk_live_51H8abcDEF123456789';
```
OK:
```ts
const apiKey = process.env.STRIPE_KEY;
```

## Implementation notes
Regex plus entropy filter; anchor to the line. Equivalent: gitleaks, `no-secrets`. Token storage in web storage: `token-in-web-storage`.

## Required tests
AWS key (finding), private key header (finding), env read (no finding), `password = 'changeme'` (no finding), fake test value (no finding).

## Why deterministic
Secret formats and name+entropy heuristics are textual; placeholders are excluded by explicit lists.
