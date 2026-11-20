# typescript/job-queue-retry-and-locks: BullMQ job retry idempotency, lock expiry and failure observation

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: RabbitMQ and background jobs; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
Job queues retry failed jobs and re-run stalled ones. A job whose lock expires while the original worker still runs can execute twice concurrently. Retention and dedup lifetimes and unobserved worker failures also matter.

## Why AI
Depends on the library's actual options (`attempts`, `lockDuration`, `removeOnComplete`, `jobId` dedup) and the job's runtime.

## AI prompt
"With the shown options, can the job run twice (retry after partial effect, stalled-job recovery while the original still works)? Is the effect idempotent? Do `removeOnComplete`/dedup lifetimes allow repeat effects? Are worker `failed`/`error` events observed? Give the concrete duplicate effect." Evidence: processor, Queue/Worker options, job duration, side effects.

## Do not flag
- Idempotent processors, or jobs shorter than lock duration with renewals.

## Examples
Flag: processor sends an email, `attempts: 5`, throws after sending, no sent-marker.
OK: sent-marker stored before retrying; stalled-job duplicates impossible due to idempotent key.

## Evaluation cases
1. 90 s job, `lockDuration` 30 s, no renewal, non-idempotent effect: flag.
2. Idempotent upsert processor with retries: ok.
3. No `worker.on('failed')` and failures needed alerting: flag.
