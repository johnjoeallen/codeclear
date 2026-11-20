# sql/rolling-deploy-incompatibility: Migration breaks the previous application version

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | MIGRATION |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | sql-migration-review: rolling deployment compatibility (nullable columns, defaults, renamed fields, removed constraints) |

## Intent
During a rolling deployment the old application version runs against the new schema. A migration that only works with the new code (new required column, changed default, renamed field, dropped constraint the old code relies on) causes failures until the rollout finishes, or on rollback.

## Why AI
Whether the old version breaks depends on how the old code reads and writes the affected columns, which is outside the SQL file. The deterministic subsets are covered by `sql/add-not-null-column-without-default` and `sql/destructive-schema-change`; this rule covers the remaining cases (nullability changes, default changes, constraint removal or tightening, type changes).

## AI prompt
"This migration will run while the previous application version is still serving traffic. Using the supplied code, name one concrete statement from the previous version (insert, update, select or constraint reliance) that fails or returns wrong data after this migration. Check: columns made NOT NULL or given/removed defaults, renamed or retyped columns, removed or added constraints and unique indexes. If you cannot name such a statement, report nothing."
Evidence: the migration diff, the previous-version code that touches the affected tables (ORM entities, repositories, SQL), and the deployment strategy if stated. Only the diff and files supplied may be used; do not assume clients that are not shown.

## Do not flag
- Changes already handled by the expand/contract pattern (new nullable column, additive default).
- Objects the previous version provably does not use.
- Findings already reported by `sql/add-not-null-column-without-default` or `sql/destructive-schema-change`.
- Generic preference for a migration tool or database.

## Examples
Flag: `ALTER TABLE orders ALTER COLUMN note SET NOT NULL;` while the previous `OrderService` inserts orders without `note`.
OK: `ALTER TABLE orders ADD COLUMN note TEXT;` (nullable, unused by old code).

## Evaluation cases
1. `DROP CONSTRAINT uq_email` while old code relies on duplicate-key errors for uniqueness: flag.
2. `ALTER COLUMN status SET DEFAULT 'NEW'`, old code always writes status: ok.
3. Rename `name` to `full_name`, old entity maps `name`: flag.
4. New nullable column, no code references: ok.
