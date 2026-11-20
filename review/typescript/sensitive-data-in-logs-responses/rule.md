# typescript/sensitive-data-in-logs-responses: Credentials or personal data in logs, errors or responses

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Logging whole request objects, headers, tokens or user records, or returning `err.stack`/DB errors to clients, leaks credentials and personal data.

## Detect
- `console.*`, `logger.*` called with `req`, `req.headers`, `req.body`, `headers.authorization`, an identifier matching `password|token|secret|apiKey|ssn|card`, or a whole user/entity object.
- `res.send/json` containing `err.stack`, `err.message` from a DB/driver error, or `error` objects directly.

## Do not flag
- Redacted objects (redaction config such as pino `redact`), masked values, non-production-only debug flagged by env check.

## Examples
Flag:
```ts
logger.info('login', { body: req.body });
```
OK:
```ts
logger.info('login', { userId: user.id });
```

## Implementation notes
Anchor to the logging or response call.

## Required tests
Logging `req.body` (finding), logging `userId` only (no finding), `res.json({ stack: err.stack })` (finding), pino with `redact` covering the field (no finding).

## AI prompt
"Does the logged or returned value contain credentials or personal data in practice? Report the exposure, not the API name." Evidence: call, object shape, redaction config.

## Why hybrid
The sink is syntactic; whether the object holds sensitive data requires shape knowledge.
