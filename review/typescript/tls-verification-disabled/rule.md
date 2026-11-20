# typescript/tls-verification-disabled: TLS certificate verification turned off

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | regex |
| Source | node-review: Security |

## Intent
Disabling certificate verification allows man-in-the-middle attacks on all affected connections.

## Detect
- `rejectUnauthorized: false` in an options object (`https.Agent`, `tls.connect`, `axios`, `pg` `ssl`, `ioredis` `tls`).
- Assignment `process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'` (or `"0"`/`0`).
- `strictSSL: false`, `checkServerIdentity: () => undefined`.

## Do not flag
- Files under `test/`, `__tests__`, `*.spec.*`, `*.test.*`, local development fixtures.
- Conditions explicitly bound to `NODE_ENV === 'development'` only.

## Examples
Flag:
```ts
new https.Agent({ rejectUnauthorized: false });
```
OK:
```ts
new https.Agent({ ca: fs.readFileSync(CA_PATH) });
```

## Implementation notes
Regex over source; anchor to the matched line. Exclude comments.

## Required tests
`rejectUnauthorized: false` (finding), env var `'0'` (finding), `true` (no finding), test-file match (no finding).

## Why deterministic
The literal option values are exact textual patterns with no dependence on context beyond file path.
