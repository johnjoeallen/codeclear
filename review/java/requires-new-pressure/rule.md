# java/requires-new-pressure: REQUIRES_NEW in repeated work

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | PERFORMANCE |
| Severity | LOW |
| Confidence | LOW |
| Detection type | hybrid |
| Source | java-bug-hunter: Transactions and persistence boundaries |

## Intent
`Propagation.REQUIRES_NEW` suspends the outer transaction and takes a second connection. Called per item in a loop or batch it can exhaust the connection pool or deadlock against the parent's locks, and is only needed if the record must survive parent rollback.

## Detect
A method annotated `@Transactional(propagation = REQUIRES_NEW)` that is called from inside a loop, stream lambda or batch/listener method, or from another `@Transactional` method that holds locks.

## Do not flag
- A single call outside loops.
- Audit/failure records that deliberately must persist after parent rollback (comment or naming evidence).

## Examples
Flag:
```java
@Transactional void process(List<Item> items) { for (var i : items) audit.record(i); } // audit.record is REQUIRES_NEW
```
OK: one `REQUIRES_NEW` call per request for an audit that must survive rollback.

## Implementation notes
AST call graph restricted to the same or visible classes. Anchor to the loop call.

The callee is found with the project call graph (`targetAnnotations`): a method whose `@Transactional` has `REQUIRES_NEW`. The call must be in a loop, a lambda, or a method annotated as a listener or scheduled job. A callee whose name contains `audit`, `failure`, `error`, `dead`, `dlq`, `log` or `record` is treated as a deliberate record. A caller that holds locks is not detected.

## Required tests
REQUIRES_NEW in a loop (candidate), outside loop (none), plain `REQUIRED` (none).

## Why hybrid
Pool pressure depends on batch size and pool configuration.

## AI prompt
"Is the REQUIRES_NEW work needed to survive a parent rollback, and can the repeated use realistically exhaust the pool or deadlock? Report only with the repetition count source and pool size when shown." Evidence: loop/caller, callee annotation, datasource pool settings.
