# sql/blocking-backfill-or-constraint: Backfill or constraint change that locks or scans a large table

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | hybrid |
| Source | sql-migration-review: backfills or index/constraint changes that lock or scan a large table |

## Intent
A single-statement backfill or a validating constraint holds locks and scans the whole table, blocking writes for the duration. Index creation is covered by `sql/blocking-index-creation`.

## Detect
Candidates in a table not created in the same file:
- `UPDATE <table> SET ...` over many rows in one statement (no batching, `LIMIT`, key range or loop with commit); an unfiltered `UPDATE` also matches `sql/update-or-delete-without-where`.
- PostgreSQL `ADD CONSTRAINT ... FOREIGN KEY|CHECK` without `NOT VALID`; `SET NOT NULL`; `ALTER COLUMN ... TYPE`.
- MySQL/SQL Server `ALTER TABLE` that rewrites the table (type change, add constraint with check).

## Do not flag
- Tables created in the same file.
- Statements preceded by a size hint such as `-- table-size: small`.
- Constraints added `NOT VALID` and validated separately, batched backfills.

## Examples
Flag: `ALTER TABLE orders ADD CONSTRAINT fk_c FOREIGN KEY (customer_id) REFERENCES customers (id);`
OK: same statement with `NOT VALID`, followed by `VALIDATE CONSTRAINT`.

## Implementation notes
Tokenise statements; anchor to the statement start. Emit the table name and statement kind as evidence for the AI step.

## Required tests
Unbatched backfill (candidate), `NOT VALID` constraint (no candidate), new-table statement (no candidate), size hint (no candidate).

## AI prompt
"Is the table touched by this statement plausibly large or heavily written, according to the evidence supplied? Would the statement hold a lock or scan long enough to block writes with no bounded deployment path (batching, online/concurrent option)? Report only if size or usage evidence supports it; otherwise drop the candidate."
Evidence: the statement, table name, any row-count, size or traffic hints in the MR, related migrations, and the migration tool's transaction behaviour if visible.

## Why hybrid
Locking statements are syntactically detectable, but whether they hurt depends on table size and usage, which only the reviewer can judge from MR evidence.
