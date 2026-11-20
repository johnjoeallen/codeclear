# typescript/retry-non-idempotent-or-amplifying: Retries that duplicate writes or amplify load

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects; node-bug-hunter: Distributed workflows and microservices |

## Intent
A timeout can occur after the downstream write committed, so retrying a non-idempotent call duplicates it. Unbounded or nested retries (client, wrapper, queue) multiply traffic and outlive the original deadline.

## Why AI
Needs the downstream's idempotency, the attempt count and timing across layers, and which errors are transient.

## AI prompt
"List the retry layers and their attempts/timeouts. Does any retried call create a side effect that may already have committed (timeout after write), without an idempotency key? Does total attempt time exceed the caller's deadline, or multiply requests? Report only with visible attempt/timing evidence." Evidence: retry code/config at each layer, downstream API contract, deadline.

## Do not flag
- Retries of reads or idempotent calls, or retries with idempotency keys, bounded with backoff and jitter.

## Examples
Flag:
```ts
retry(() => axios.post('/charge', body), { retries: 5 }) // no idempotency key
```
OK: same call with `Idempotency-Key` header and 3 bounded retries.

## Evaluation cases
1. POST create with 5 retries, no key: flag.
2. GET with backoff: ok.
3. Client retries 3x inside a queue worker that retries 5x with 10s deadline: flag.
