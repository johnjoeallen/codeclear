# java/multi-step-write-partial-success: Multi-step write can partially succeed

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts |

## Intent
A workflow of several writes without a covering transaction or compensation leaves inconsistent data when a later step fails. Local database rollback does not undo remote side effects.

## Why AI
Whether a failure window exists depends on transaction boundaries, which steps are remote, and what compensation or retry exists, often across several classes.

## AI prompt
"List the write steps in order and mark each as local-transactional or remote. For a specific failing step, state the resulting state and whether rollback or compensation restores it. Report only with a concrete failing step and the inconsistent end state." Evidence: the method, callees that write, `@Transactional` configuration, compensation or retry code.

## Do not flag
- Steps within one effective transaction that all roll back.
- Idempotent, retried flows with visible reconciliation.

## Examples
Flag: `orderRepo.save(o); inventoryClient.reserve(o); paymentRepo.save(p);` with no transaction; failure at `reserve` leaves an orphan order.
OK: all writes in one `@Transactional` method, remote call performed last with compensation on failure.

## Evaluation cases
1. Two repo saves, no `@Transactional`, second throws - report.
2. Two repo saves in `@Transactional`, exception propagates - no report.
3. Repo save then remote call that fails, no compensation - report.
4. Remote call fails but transaction not yet written anything - no report.
