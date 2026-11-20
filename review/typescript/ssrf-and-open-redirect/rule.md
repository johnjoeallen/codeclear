# typescript/ssrf-and-open-redirect: Server-side request or redirect to an untrusted URL

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Fetching a user-supplied URL lets attackers reach internal services and metadata endpoints; following redirects can bypass host checks; redirecting to a user URL enables phishing.

## Detect
`fetch/axios/got/http(s).request/request` whose URL argument derives from `req.*` or message fields, without an allow-list of hosts and a block of private ranges; redirect following left on after a host check; `res.redirect(req.query.*)`.

## Do not flag
- URLs built from constants with user values only in encoded path segments.
- Hosts matched against an allow-list after parsing with `new URL`, relative-only redirect targets.

## Examples
Flag:
```ts
const r = await fetch(req.query.url as string);
```
OK:
```ts
const u = new URL(raw); if (!ALLOWED.has(u.hostname)) throw new Error('blocked');
```

## Implementation notes
Anchor to the request call. Taint-based; no standard ESLint equivalent.

Detected on `fetch`, `axios.*`, `got`, `request`, `needle`, `superagent` and `http(s).request/get` calls, and on `redirect(...)`, whose first argument is not a literal and is derived from `req`, `request`, `message` or `query` (one step through a variable). A function that mentions an allow-list (`allowlist`, `allowedHosts`, `ALLOWED`), `isPrivate`, `ipaddr`, `isIP` or `dns.lookup` is treated as checking hosts; for a redirect, `startsWith("/")` or `new URL` counts as a check.

## Required tests
Request URL to `fetch` (finding), allow-list check (no finding), `redirect(req.query.next)` (finding), constant URL (no finding).

## AI prompt
"Is the URL attacker-controlled; does an allow-list on the parsed hostname (not a substring) exist; are redirects and DNS resolution to private IPs handled?" Evidence: call, URL origin, validation, redirect options.

## Why hybrid
Sinks are syntactic; reachability and validation strength need flow and context.
