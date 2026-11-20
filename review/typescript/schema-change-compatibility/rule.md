# typescript/schema-change-compatibility: Model or schema change incompatible with constraints or old code

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Databases, ORM, and transactions |

## Intent
A changed entity/model must match the visible constraints and migrations, and work while old and new application versions run together (new required column, renamed field, dropped column).

## Why AI
Requires migration files, deploy ordering and the constraints, none of which the model file shows. Do not assume deployment order or unseen migration contents.

## AI prompt
"Compare the model change with the migrations and constraints supplied. Name an insert/update from the old or new version that fails or writes wrong data during a rolling deploy. If the migration is not supplied, report nothing." Evidence: model diff, migrations, constraints, deployment strategy.

## Do not flag
- Additive nullable columns, or models with a supplied compatible migration.

## Examples
Flag: model adds `@Column({ nullable: false }) tenantId` with no default while old pods still insert without it.
OK: column added nullable with backfill, enforced in a later release.

## Evaluation cases
1. New NOT NULL column without default, old writer still deployed: flag.
2. Nullable column with default: ok.
3. Model change, no migration supplied: ok (unknown).
