# typescript/sqs-ack-and-partial-batch: SQS delete timing, partial batch response and visibility

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless; node-bug-hunter: AWS and serverless failure windows |

## Intent
Each record's completed work must map to deletion or a partial-batch response. Throwing for the whole batch retries completed records; deleting before processing loses failures; processing that outlasts the visibility timeout is redelivered concurrently. FIFO ordering/dedup and poison-message routing must match the handler.

## Why AI
Depends on event source mapping (`ReportBatchItemFailures`), visibility timeout, DLQ/redrive and FIFO settings.

## AI prompt
"For each record, trace work done versus delete/`batchItemFailures`. Does one failure cause completed records to rerun, or failed records to be dropped? Can processing exceed the visibility timeout? Are poison messages routed to a DLQ? Give the loss or duplicate scenario using the supplied configuration." Evidence: handler, event-source mapping config, queue attributes, idempotency of work.

## Do not flag
- Idempotent records with whole-batch retry acceptable; handlers returning `batchItemFailures` correctly.

## Examples
Flag:
```ts
for (const r of event.Records) await work(r); // throw on 3rd redoes 1st and 2nd; no batchItemFailures
```
OK: collect failures and `return { batchItemFailures }`.

## Evaluation cases
1. Throw on first failure, non-idempotent work, no partial response: flag.
2. `batchItemFailures` returned and mapping enables it: ok.
3. Handler 120 s, visibility 30 s: flag.
