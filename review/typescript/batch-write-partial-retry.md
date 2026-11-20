# typescript/batch-write-partial-retry: Retrying a whole batch after partial success

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-bug-hunter: Database invariants and rollback |

## Intent
In a non-transactional batched write, earlier chunks may already be committed when a later one fails. Retrying the whole input repeats non-idempotent operations.

## Why AI
Needs the idempotency of each operation and whether the batch is atomic.

## AI prompt
"Map each chunk's success to the retry input. If the batch is not atomic and retry restarts from the beginning, name the non-idempotent effect that is repeated." Evidence: batch loop, retry wrapper, operation idempotency, transaction scope.

## Do not flag
- Atomic batches (single transaction), or idempotent upserts.

## Examples
Flag:
```ts
for (const c of chunks) await insertMany(c); // retried from start by caller on error
```
OK: record last completed chunk and resume; or use idempotent keys.

## Evaluation cases
1. Chunked inserts with counters incremented, retry restarts: flag.
2. Chunked `upsert` on a unique key: ok.
3. Single transaction wrapping all chunks: ok.
