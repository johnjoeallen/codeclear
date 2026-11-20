# typescript/http-client-no-timeout: Outbound HTTP/gRPC call without a bounded timeout

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: Microservices and distributed side effects |

## Intent
Without a timeout a slow dependency holds sockets and request slots until the platform kills them, cascading failure. (Status checks on `fetch`: `fetch-without-error-handling`.)

## Detect
In server code: `fetch(url, opts)` with no `signal`; `axios.*`/`axios.create` with no `timeout`; `got`/`node-fetch`/`http.request` without a `timeout`/`signal`; gRPC call without `deadline`.

## Do not flag
- Clients configured once with a default timeout (`axios.create({ timeout })`) used by the call.
- Calls given `AbortSignal.timeout(...)`.
- Browser-only code.

## Examples
Flag:
```ts
const r = await fetch(url);
```
OK:
```ts
const r = await fetch(url, { signal: AbortSignal.timeout(3000) });
```

## Implementation notes
Anchor to the call. Resolve same-file client instances for default options.

## Required tests
`fetch` with no signal (finding), with `AbortSignal.timeout` (no finding), axios instance with `timeout` (no finding), axios call without (finding).

## Why deterministic
Presence of a timeout/signal option on the call or its same-file client instance is syntactic.
