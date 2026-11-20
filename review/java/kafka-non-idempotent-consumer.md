# java/kafka-non-idempotent-consumer: Redelivery duplicates a side effect

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Kafka; java-bug-hunter: RabbitMQ |

## Intent
Brokers deliver at least once. A consumer whose side effect (insert, charge, email, counter increment) is not idempotent applies it twice on redelivery or rebalance.

## Why AI
Whether the operation is naturally idempotent, or guarded by a unique key or dedup table, requires understanding the operation and schema.

## AI prompt
"Assume this record/message is delivered twice. Identify the side effects. Report only if a second delivery produces a duplicate or wrong effect and no dedup key, unique constraint, upsert or idempotent operation is shown; name the effect." Evidence: listener body, repository/schema (unique constraints), outbound calls, message id fields.

## Do not flag
- Upserts, `INSERT ... ON CONFLICT`, set-state operations, dedup by event id.
- Read-only or purely idempotent consumers.

## Examples
Flag: `account.balance += event.amount; repo.save(account);` per record without event-id check.
OK: processed-event table with unique `event_id` inserted in the same transaction.

## Evaluation cases
1. Balance increment on each record, no id check - report.
2. Insert with unique constraint on event id, duplicate handled - no report.
3. Sends email per message without dedup - report.
4. `UPDATE status = 'DONE'` set-state - no report.
