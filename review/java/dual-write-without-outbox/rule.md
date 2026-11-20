# java/dual-write-without-outbox: Database write and broker/search publish not atomic

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Transactions and persistence boundaries; java-bug-hunter: Kafka; java-bug-hunter: Spring and distributed-service behavior |

## Intent
A database transaction does not make a Kafka/RabbitMQ publish, search-index write or other remote write atomic with it. A crash or failure between them loses or duplicates the event, or publishes data that was rolled back.

## Why AI
The failure window depends on transaction manager configuration, listener phase (`@EventListener` vs `@TransactionalEventListener`), existence of an outbox/compensation/reconciliation and eventual-consistency expectations outside the method.

## AI prompt
"Trace the DB write and the remote publish/index write. State what happens on failure between them and on rollback after publish. Look for outbox table, transaction-synchronised publish (AFTER_COMMIT), compensation, retry, or Kafka transactions. Report only if no effective mechanism is shown, naming the failing step and resulting inconsistency. Do not assume `@Transactional` alone provides atomicity." Evidence: the method and listeners, producer/transaction configuration, outbox code.

## Do not flag
- Outbox pattern with relay, publish in AFTER_COMMIT with accepted loss window documented, or idempotent consumers plus reconciliation.
- Fire-and-forget notifications explicitly non-critical.

## Examples
Flag: `@Transactional` method calls `repo.save(o); kafka.send("orders", o);`.
OK: save and insert into `outbox` in one transaction; a relay publishes.

## Evaluation cases
1. Save then `kafkaTemplate.send` in same transactional method, no outbox - report.
2. `@TransactionalEventListener(AFTER_COMMIT)` publishing, failure acknowledged by retry store - no report.
3. Elasticsearch index after commit with no retry or reconciliation, search required to be consistent - report.
4. Event published for audit only, loss acceptable per comment - no report.
