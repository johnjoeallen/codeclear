# java/lost-update-read-modify-write: Unprotected read-modify-write on a shared row

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Transactions and persistence boundaries; java-bug-hunter: AWS SDK |

## Intent
Reading a row (balance, counter, stock, DynamoDB item), changing it in memory and writing it back lets concurrent requests overwrite each other unless locking, version checks, conditional writes or atomic updates are used.

## Why AI
Whether two writers can really hit the same row, and whether `@Version`, isolation level, `SELECT ... FOR UPDATE` or conditional expressions protect it, spans entity mappings, queries and configuration.

## AI prompt
"Identify the read-modify-write sequence. Check for `@Version`, pessimistic lock, atomic update query, conditional DynamoDB `ConditionExpression`, or serialised access. Report only with two concurrent requests and the resulting lost value." Evidence: the method, entity (version field), repository queries, isolation settings, DynamoDB request.

## Do not flag
- Entities with `@Version`, locking queries, atomic `UPDATE ... SET x = x + :d`, conditional writes.
- Rows that are only written by one single-threaded process (stated).

## Examples
Flag: `var a = repo.findById(id).get(); a.setBalance(a.getBalance().subtract(x)); repo.save(a);` with no `@Version`.
OK: `@Query("update Account set balance = balance - :x where id = :id and balance >= :x")`.

## Evaluation cases
1. Balance decrement via findById/save, no lock - report.
2. Same with `@Version` on entity - no report.
3. DynamoDB `getItem` then `putItem` without condition, concurrent consumers - report.
4. `findById` then update guarded by `PESSIMISTIC_WRITE` - no report.
