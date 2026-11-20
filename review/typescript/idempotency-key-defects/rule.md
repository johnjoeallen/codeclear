# typescript/idempotency-key-defects: Idempotency key scope, reservation or replay handled incorrectly

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects; node-bug-hunter: Distributed workflows and microservices |

## Intent
An endpoint that promises deduplication must scope keys (tenant/operation), reserve them atomically, detect payload conflicts, replay the original result, and not leave a key claiming success after a failed write or expire it too early.

## Why AI
Correctness is defined by the endpoint's promise and requires the key store, write ordering and TTL together.

## AI prompt
"Check: key scope; atomic reservation (check-then-set race); same key with a different payload; replay returns the stored result; failed business write releases or marks the key failed; TTL not shorter than the client retry window. Give the sequence producing a duplicate effect or a permanently failed key." Evidence: handler, key store code, TTL, client retry policy.

## Do not flag
- Endpoints that make no idempotency promise.
- Natural idempotency via a unique constraint.

## Examples
Flag:
```ts
if (await store.get(key)) return cached; await store.set(key, 'done'); await charge(); // claims done before charging
```
OK: `SET key pending NX`, charge, then set the result; on failure delete the key.

## Evaluation cases
1. Key set to success before the write completes: flag.
2. Check-then-set non-atomic on concurrent duplicates: flag.
3. Atomic `NX` reservation, result stored after commit, cleanup on error: ok.
