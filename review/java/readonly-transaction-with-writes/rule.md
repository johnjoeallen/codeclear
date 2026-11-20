# java/readonly-transaction-with-writes: Write inside a readOnly transaction

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Transactions and persistence boundaries |

## Intent
`@Transactional(readOnly = true)` is a hint that may skip flushes or route to a replica, so writes can be silently dropped or rejected.

## Detect
A method (or one in a `readOnly = true` class) that calls `save`, `saveAll`, `delete*`, `persist`, `merge`, `flush`, `update`/`insert` JDBC, `@Modifying` repository methods or setters on managed entities.

## Do not flag
- Methods overriding `readOnly` with `@Transactional` without it.
- Writes to non-transactional resources (cache, metrics).
- Calls annotated `REQUIRES_NEW` that open their own writable transaction.

## Examples
Flag:
```java
@Transactional(readOnly = true) Order get(long id) { o.setLastRead(now()); return repo.save(o); }
```
OK: separate non-readOnly method for the write.

## Implementation notes
AST with repository type resolution. Anchor to the write call.

A write is recognised by the declared type of the receiver: a type ending in `Repository`, `EntityManager`, `Dao` or `DAO` for `save*`, `delete*`, `persist`, `merge`, `flush` and `remove`, and a type containing `Jdbc` for `update` and `batchUpdate`. `@Modifying` repository methods and setters on managed entities are not detected.

## Required tests
`save` in readOnly (candidate), read-only query (none), method-level override (none).

## Why hybrid
Whether the call is truly a persistent write depends on callee implementation.

## AI prompt
"Does this readOnly transaction perform or depend on a persisted write? State the write and the expected-vs-actual outcome." Evidence: the method, callee repository/service code, transaction config.
