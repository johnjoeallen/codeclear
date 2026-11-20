# typescript/token-in-web-storage: Credential stored in localStorage or sessionStorage

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | typescript-review: Security |

## Intent
Web storage is readable by any script on the page, so one XSS flaw exposes a stored token.

## Detect
`localStorage.setItem(key, ...)` or `sessionStorage.setItem(key, ...)`, or property assignment
`localStorage.key = ...`, where the key matches (case-insensitive)
`token|jwt|secret|password|credential|auth|session|api[-_]?key`.

## Do not flag
- Keys that are clearly non-sensitive (`theme`, `locale`, `authorName`): require the whole word to match.
- Test files.

## Examples
Flag:
```ts
localStorage.setItem('accessToken', token);
```
OK:
```ts
localStorage.setItem('theme', 'dark');
```

## Implementation notes
AST for `setItem` calls with string-literal key; fall back to regex on dynamic keys with `confidence: LOW`.

Detected on `localStorage`/`sessionStorage` (and `window.` forms) `setItem` calls and on property or index assignments. A key matches when a sensitive word is a whole word of it: split by a non-letter (`x-auth-token`, `auth_token`) or by a capital (`accessToken`), so `authorName` and `theme` do not match.

## Required tests
`accessToken` (finding), `theme` (no finding), `sessionStorage` token (finding), dynamic key (no finding).
