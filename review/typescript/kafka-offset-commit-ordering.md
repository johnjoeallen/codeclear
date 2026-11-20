# typescript/kafka-offset-commit-ordering: Offset commit not aligned with completed processing

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
Offsets must be committed only after processing and the database commit succeed. Auto-commit, early `commitOffsets` or `resolveOffset` ahead of work lose messages; committing a later offset in a batch skips an earlier failed record.

## Why AI
Behavior depends on the client (kafkajs `autoCommit`, `eachBatch` resolve semantics, `eachBatchAutoResolve`), its configuration and the handler's failure behavior.

## AI prompt
"Draw the sequence: fetch, process, DB commit, offset commit. Inject a failure at each boundary and state whether work is lost or duplicated. In batches, does acknowledging a later offset skip an earlier failure? Use the visible client configuration; do not infer delivery guarantees from the handler alone." Evidence: consumer code, client config (`autoCommit`, `eachBatchAutoResolve`), DB commit code.

## Do not flag
- Commit after awaited processing, with errors propagating to the client's retry.

## Examples
Flag:
```ts
await consumer.run({ autoCommit: true, eachMessage: async ({ message }) => { process(message); } }); // not awaited
```
OK: `await handle(message)` completes DB commit before returning; errors thrown.

## Evaluation cases
1. `resolveOffset` called before the DB write: flag.
2. Handler catches and logs, offset advances: flag.
3. Awaited handler, errors rethrown, default auto-resolve: ok.
