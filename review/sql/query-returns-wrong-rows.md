# sql/query-returns-wrong-rows: Changed query returns wrong rows for a given input

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | sql-migration-review: changed queries ... use a query shape that will return wrong rows for an evidenced input |

## Intent
A changed query whose joins, predicates or NULL handling produce duplicates, missing rows or extra rows for inputs that occur in practice. Injection through concatenation is covered by `sql/dynamic-sql-concatenation`.

## Why AI
Correctness depends on the data model (keys, cardinality, nullability) and on the intended result, none of which are in the statement text.

## AI prompt
"For this changed query, give one concrete input row set (from the supplied schema or data) for which it returns wrong rows: duplicate rows from a join on a non-unique key, rows lost to `NOT IN`/`=` with NULL, an `OUTER JOIN` filtered in `WHERE`, an `OR` without parentheses, an off-by-one or inclusive/exclusive range, or a changed filter that no longer matches the stated purpose. Show the input and the wrong result. If no such input is evidenced, report nothing."
Evidence: the query diff, table definitions and constraints, the caller or intent (MR description, ticket), sample data if supplied.

## Do not flag
- Style, readability or performance preferences.
- Edge cases for data that the schema prevents.
- Queries unchanged by the MR.

## Examples
Flag: `SELECT * FROM a WHERE id NOT IN (SELECT a_id FROM b)` where `b.a_id` is nullable: returns no rows once one NULL exists.
OK: same query using `NOT EXISTS`.

## Evaluation cases
1. `LEFT JOIN b ... WHERE b.status = 'X'` meant to keep unmatched rows: flag.
2. Join on `customer.email` where email is not unique and the result is counted: flag.
3. `WHERE a = 1 AND b = 2 OR c = 3` replacing a parenthesised filter: flag.
4. `WHERE created_at >= :from AND created_at < :to`: ok.
