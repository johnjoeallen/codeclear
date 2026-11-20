# sql/destructive-schema-change: Drop, rename or truncate

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | sql-migration-review: rolling deployment compatibility, irreversible changes |

## Intent
Dropping or renaming a column or table breaks any running application version that still uses it, and cannot be undone by rolling back the code.

## Detect
`DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, `ALTER TABLE ... RENAME` (table or column), `ALTER COLUMN ... TYPE` narrowing a type, `DROP CONSTRAINT`.

## Do not flag
- `DROP TABLE IF EXISTS` on a table created in the same file (temporary or staging).
- Statements on tables whose name starts with `tmp_` or `temp_`.
- Files in a rollback/`down`/`undo` migration path.

## Examples
Flag:
```sql
ALTER TABLE users DROP COLUMN legacy_id;
```
OK:
```sql
ALTER TABLE users ALTER COLUMN legacy_id DROP NOT NULL;
```

## Implementation notes
Regex finds the candidate statement; the AI step confirms that a running version still uses the object (expand/contract pattern).

## Required tests
`DROP COLUMN` (finding), `RENAME COLUMN` (finding), temp table drop (no finding), file under `down/` (no finding).

## AI prompt
"Does the previous application version, or any other consumer shown, still read or write the dropped, renamed or narrowed object? Is the data unrecoverable once the statement runs? Report only if you can name the consumer or the unrecoverable data."
Evidence: the statement, code referencing the table or column, earlier migrations in the same MR (a prior contract step).

## Why hybrid
The statement is syntactically certain, but harm depends on whether any running version or consumer still uses the object.
