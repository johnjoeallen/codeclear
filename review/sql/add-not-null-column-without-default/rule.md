# sql/add-not-null-column-without-default: NOT NULL column added with no default

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | regex |
| Source | sql-migration-review: rolling deployment compatibility |

## Intent
Adding a `NOT NULL` column with no default fails on a table that has rows. During a rolling deploy, old application code that does not write the column then fails its inserts.

## Detect
`ALTER TABLE ... ADD [COLUMN] <name> <type> ... NOT NULL` where the same column definition has no `DEFAULT` clause and is not `GENERATED`/`IDENTITY`/`SERIAL`.

## Do not flag
- A `CREATE TABLE` in the same file as the `ALTER` (new empty table).
- A column that has `DEFAULT`.
- A migration that adds the column as nullable, backfills, and later sets `NOT NULL` in separate statements.

## Examples
Flag:
```sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL;
```
OK:
```sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL DEFAULT 'EU';
```

## Implementation notes
Parse statements (split on `;`, strip comments) before matching. Dialects differ; start with PostgreSQL, MySQL and SQL Server syntax.

The SQL analyzer is a tokenizer: it removes comments, splits statements at `;` and at a `GO` line, and cuts an `ALTER TABLE` into its `ADD` definitions at top-level commas, so each column of a multi-column `ADD` is checked on its own line. Constraints (`ADD CONSTRAINT`, `PRIMARY KEY`, ...) are not columns. A dollar-quoted body (`$$ ... $$`) belongs to the statement that opens it, and its code is checked line by line.

## Required tests
Missing default (finding), with default (no finding), `IDENTITY` (no finding), multi-column `ADD` (finding for the right column), commented-out statement (no finding).

## Why deterministic
The statement text alone shows a `NOT NULL` column with no `DEFAULT`; no runtime or intent context is needed.
