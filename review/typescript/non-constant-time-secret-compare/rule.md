# typescript/non-constant-time-secret-compare: Secrets or signatures compared with ordinary equality

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: Security |

## Intent
`===` on HMACs, API keys or tokens returns early at the first differing byte, enabling timing attacks.

## Detect
`==`, `===`, `!=`, `!==` (or `.equals`) where one operand is an identifier or property named `signature`, `sig`, `hmac`, `digest`, `apiKey`, `token`, `secret`, `hash` (excluding `*Id`) or the result of `createHmac(...).digest(...)`, and the other is request-derived or computed.

## Do not flag
- `crypto.timingSafeEqual`, bcrypt/argon2 `compare`, comparisons to `undefined`/`null`/empty string.
- Equality of non-secret identifiers.

## Examples
Flag:
```ts
if (req.headers['x-signature'] === expectedSignature) {}
```
OK:
```ts
crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
```

## Implementation notes
Anchor to the comparison. Remember to length-check before `timingSafeEqual`.

Detected on `==`, `===`, `!=`, `!==`. An operand is a secret when its name (identifier, property or string index) ends in `signature`, `sig`, `hmac`, `digest`, `apiKey`, `token`, `secret` or `hash`, or it is a `createHmac(...).digest(...)` chain; `tokenId` and similar do not end in the word. The other operand must not be a literal, `null` or `undefined`. `.equals(...)` is not looked at.

## Required tests
`sig === expected` (finding), `timingSafeEqual` (no finding), `token === undefined` (no finding), `userId === id` (no finding).

## Why deterministic
Operand naming and the comparison operator are syntactic; the name list is the contract.
