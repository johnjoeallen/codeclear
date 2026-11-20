# typescript/promise-all-sibling-continues: Promise.all rejection leaves siblings running

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
`Promise.all` rejects at the first failure but does not cancel the rest. Siblings can write late, duplicate effects after a retry, or use a released transaction client.

## Why AI
Harm depends on whether siblings have side effects, share a transaction, and what the caller does after rejection.

## AI prompt
"When one branch rejects, which sibling operations still run, and what do they do (write, charge, use a released client) after the caller reports failure or retries? Report only with that scenario." Evidence: the `Promise.all` call, each branch's side effects, the error handler and retry logic.

## Do not flag
- Read-only or idempotent branches.
- Branches that share an `AbortSignal` that is aborted on failure.

## Examples
Flag:
```ts
await Promise.all(orders.map((o) => ship(o))); // one failure, others still ship, caller retries all
```
OK:
```ts
const results = await Promise.allSettled(orders.map(ship)); retryOnlyFailed(results);
```

## Evaluation cases
1. `Promise.all` of writes inside a transaction callback: flag.
2. `Promise.all` of pure reads: ok.
3. `allSettled` result reported as blanket success: see `allsettled-ignored-rejections`.
