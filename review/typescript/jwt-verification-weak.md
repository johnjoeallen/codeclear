# typescript/jwt-verification-weak: Token verification without algorithm, issuer, audience or expiry enforcement

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | HIGH |
| Detection type | ast |
| Source | node-review: Security |

## Intent
Decoding without verifying, accepting `alg: none`, ignoring expiry, or not pinning issuer/audience lets forged or foreign tokens authenticate.

## Detect
- `jwt.decode(...)` result used for authentication or authorization decisions.
- `jwt.verify(token, key, opts)` where `opts` has `ignoreExpiration: true`, `algorithms` contains `'none'`, or `algorithms` is absent when the key is a shared/public key; `complete`-decoded `header.alg` used to choose the key.
- `jose.jwtVerify` / `verify` with no `issuer`/`audience` options when the file elsewhere reads `iss`/`aud` claims.

## Do not flag
- `jwt.decode` used only for non-security display or logging.
- `verify` with pinned `algorithms`, `issuer` and `audience`, or a different verified mechanism (JWKS client with options).

## Examples
Flag:
```ts
const claims = jwt.decode(token); req.user = claims;
```
OK:
```ts
jwt.verify(token, key, { algorithms: ['RS256'], issuer: ISS, audience: AUD });
```

## Implementation notes
Anchor to the verify/decode call.

## Required tests
`decode` for auth (finding), `ignoreExpiration` (finding), pinned verify (no finding), `decode` for logging (no finding).

## Why deterministic
Option presence and API choice at a call site are syntactic; unusual protocols are excluded by the Do-not-flag list.
