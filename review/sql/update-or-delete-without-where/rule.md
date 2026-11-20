# sql/update-or-delete-without-where: UPDATE or DELETE affecting every row

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | sql-migration-review: irreversible data changes |

## Intent
An `UPDATE` or `DELETE` with no `WHERE` clause changes or removes every row. In a migration this is rarely intended and is hard to reverse.

## Detect
A statement that starts with `UPDATE <table> SET ...` or `DELETE FROM <table>` and has no `WHERE` clause (and for `UPDATE ... FROM`, no `WHERE`/`JOIN ... ON` condition).

## Do not flag
- Statements on tables created in the same file.
- Statements preceded by the comment `-- full-table: intentional`.

## Examples
Flag:
```sql
UPDATE accounts SET status = 'CLOSED';
```
OK:
```sql
UPDATE accounts SET status = 'CLOSED' WHERE closed_at IS NOT NULL;
```

## Implementation notes
Tokenise rather than regex the whole file so that a `WHERE` in a subquery or comment is not mistaken for a top-level one.

A statement is checked as a whole, so a `WHERE` in a subquery does not count (it is inside parentheses). Statements inside a dollar-quoted function body are not looked at. An `UPDATE ... FROM` or `DELETE ... FROM` that joins with `ON` is not flagged.

## Required tests
No `WHERE` (finding), `WHERE` in a subquery only (finding), top-level `WHERE` (no finding), marker comment (no finding).

## Why deterministic
A top-level statement without a `WHERE` clause is a syntactic fact; the marker comment is the explicit opt-out.
