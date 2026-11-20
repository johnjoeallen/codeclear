# typescript/scheduled-task-overlap-replicas: Scheduled or detached work duplicated, overlapping or not durable

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: RabbitMQ and background jobs |

## Intent
A cron/interval task runs on every replica and can overlap itself. Work detached from a request (fire-and-forget) is lost on restart and is not durable.

## Why AI
Needs replica count and the invariant duplicate execution would break. (Local async interval overlap: `overlapping-async-interval`.)

## AI prompt
"Given the shown deployment, does this task run on more than one instance, and does duplicate execution violate a shown invariant (double email, double charge)? Is request-detached work required to survive a restart? Name the invariant. Report nothing if neither is shown." Evidence: scheduler code, replica/cluster config, leader election or lock, invariant.

## Do not flag
- Idempotent tasks, tasks guarded by a distributed lock or leader election, best-effort work.

## Examples
Flag: `cron.schedule('0 * * * *', sendInvoices)` in a service deployed with `replicas: 3`.
OK: the same task acquiring a DB advisory lock or running in a single scheduler pod.

## Evaluation cases
1. Cron sends invoices, 3 replicas, no lock: flag.
2. Cron refreshes a local cache: ok.
3. Request handler starts a payout in an unawaited promise: flag.
