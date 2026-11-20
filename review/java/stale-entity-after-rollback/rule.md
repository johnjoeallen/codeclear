# java/stale-entity-after-rollback: In-memory entity used as if changes persisted after rollback

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Transactions and persistence boundaries |

## Intent
After a transaction rolls back, entities mutated in memory still hold the rolled-back values. A caller that returns, caches or publishes them acts as though the changes persisted.

## Why AI
Requires tracing which objects survive past the transaction and how callers use them.

## AI prompt
"Find entities or DTOs mutated inside a transactional scope that can roll back. Report only if the caller, cache or event uses them after the rollback as if persisted, naming the rollback trigger and the wrong value used." Evidence: transactional method, caller, cache/event code.

## Do not flag
- Entities discarded after a failed call, or reloaded before reuse.
- Values the caller never reads after failure.

## Examples
Flag: `order.setStatus(PAID); try { svc.persist(order); } catch (Exception e) { log.warn(...); } cache.put(order.id, order);`.
OK: cache updated only after successful commit.

## Evaluation cases
1. Mutated entity cached after caught persistence failure - report.
2. Entity reloaded after rollback - no report.
3. Exception propagates, entity unused - no report.
