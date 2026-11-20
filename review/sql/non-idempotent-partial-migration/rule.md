# sql/non-idempotent-partial-migration: Data change that can leave partially migrated data

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | sql-migration-review: non-idempotent or irreversible data changes |

## Intent
A migration that fails midway and cannot be safely re-run, or that destroys information, leaves data in a mixed state that needs manual repair.

## Why AI
Whether a failure leaves partial state depends on transaction boundaries of the migration tool, statement ordering, data contents and what is recoverable. A syntactic check cannot tell; the source also forbids assuming a migration is non-transactional without evidence. Syntactic subsets live in `sql/update-or-delete-without-where` and `sql/destructive-schema-change`.

## AI prompt
"Identify one specific failure (a statement erroring at a named point, a timeout, a re-run after partial success) after which this migration leaves data partially migrated or cannot be re-run, or after which the original data cannot be recovered. State the failing statement, the resulting data state and why re-running or rolling back does not fix it. If the tool or file shows the migration runs in a single transaction, or every step is idempotent (`IF NOT EXISTS`, guarded `WHERE`), report nothing."
Evidence: the migration file, neighbouring migrations, migration tool configuration (transactional or not, if visible), and any backup or rollback script.

## Do not flag
- Migrations proven transactional by supplied evidence, or with idempotent guards.
- Assumed non-transactional behaviour without evidence.
- Pure additive DDL.

## Examples
Flag: `UPDATE t SET b = a; ALTER TABLE t DROP COLUMN a;` run non-transactionally; a failure after the first statement leaves `a` and `b` out of step, and a re-run overwrites newer `b` values.
OK: `UPDATE t SET b = a WHERE b IS NULL;` in a transactional migration.

## Evaluation cases
1. Two data moves then `DROP TABLE old`, tool config `transactional: false`: flag.
2. Same script under a single-transaction tool: ok.
3. `INSERT ... SELECT` into new table guarded by `WHERE NOT EXISTS`: ok.
4. `UPDATE ... SET col = NULL` discarding the only copy, no backup: flag.
