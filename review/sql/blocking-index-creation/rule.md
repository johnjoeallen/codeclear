# sql/blocking-index-creation: Index created without a non-blocking option

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | hybrid |
| Source | sql-migration-review: locks on large tables |

## Intent
`CREATE INDEX` on PostgreSQL blocks writes to the table for the duration of the build. On a large table this is an outage.

## Detect
PostgreSQL: `CREATE [UNIQUE] INDEX` without `CONCURRENTLY` on a table not created in the same file.
MySQL: `ALTER TABLE ... ADD INDEX` without `ALGORITHM=INPLACE, LOCK=NONE`.
SQL Server: `CREATE INDEX` without `WITH (ONLINE = ON)` on editions that support it.

## Do not flag
- Indexes on tables created in the same file.
- Files marked with a size hint such as `-- table-size: small`.

## Examples
Flag:
```sql
CREATE INDEX idx_orders_customer ON orders (customer_id);
```
OK:
```sql
CREATE INDEX CONCURRENTLY idx_orders_customer ON orders (customer_id);
```

## Implementation notes
Table size is unknown, so mark `hybrid`: the detector finds the statement and the AI reviewer uses any size evidence in the MR. Also check that the migration tool does not wrap `CONCURRENTLY` in a transaction.

Detected on `CREATE [UNIQUE] INDEX` without `CONCURRENTLY` (or `ONLINE = ON`), and on MySQL `ALTER TABLE ... ADD INDEX/KEY` without `ALGORITHM=INPLACE|INSTANT` together with `LOCK=NONE`. The table of the index is found from `ON <table>`. A `-- table-size: small` comment directly above the statement is the size hint; a hint elsewhere in the file is not read.

## Required tests
Plain `CREATE INDEX` (finding), `CONCURRENTLY` (no finding), index on new table (no finding).

## AI prompt
"Is the indexed table plausibly large or heavily written, according to the evidence supplied? Would a blocking build cause an outage? Report only if size or usage evidence supports it; otherwise drop."
Evidence: the statement, table name, row-count or traffic hints in the MR, related migrations, migration tool transaction settings.

## Why hybrid
A blocking index statement is detectable, but its impact depends on table size and usage. Backfills and constraints are in `sql/blocking-backfill-or-constraint`.
