# typescript/cookie-cors-csrf-misconfig: Insecure cookie, session, CORS or CSRF configuration

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Session cookies without `httpOnly`/`secure`/`sameSite`, CORS that reflects any origin with credentials, proxy-trust mistakes and state-changing cookie-authenticated routes without CSRF protection expose sessions.

## Detect
- `res.cookie(name, v, opts)` / `express-session` / `cookie-session` for session or auth cookies with `httpOnly: false`, `secure: false`, or no `sameSite`.
- `cors({ origin: true })`, `origin: '*'` with `credentials: true`, or reflecting `req.headers.origin` unchecked.
- `app.set('trust proxy', true)` with no shown proxy.
- POST/PUT/DELETE routes using cookie sessions in a file with no CSRF middleware.

## Do not flag
- Non-session cookies (preferences), bearer-token APIs, development-only config.
- Settings enforced by a gateway shown in config.

## Examples
Flag:
```ts
app.use(cors({ origin: true, credentials: true }));
```
OK:
```ts
app.use(cors({ origin: ['https://app.example.com'], credentials: true }));
```

## Implementation notes
Anchor to the configuration call.

## Required tests
Reflected origin with credentials (finding), allow-list (no finding), cookie without `httpOnly` for session (finding), preferences cookie (no finding).

## AI prompt
"Which authentication model is used (cookie session vs bearer)? Does the setting enable a concrete cross-origin or session-theft attack? Do not flag public endpoints as missing authentication without a restricted-access contract." Evidence: config, auth model, proxy topology.

## Why hybrid
Option values are syntactic; exploitability depends on the auth model and deployment.
