# typescript/sql-injection-dynamic-query: SQL built from strings, including identifiers and ORDER BY

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions; node-review: Security |

## Intent
Concatenated or interpolated SQL lets input change the query. Placeholders protect values only, not table, column or `ORDER BY` identifiers.

## Detect
- `query/execute/raw/$queryRawUnsafe/sequelize.query/knex.raw` whose first argument is a template literal with `${}` or a `+` concatenation containing non-literal values.
- Dynamic identifiers or `ORDER BY ${col} ${dir}` built from request values without an allow-list.

## Do not flag
- Tagged templates that parameterize (`sql\`...\``, Prisma `$queryRaw` tagged, `knex.raw('?? = ?', [...])`).
- Interpolation of constants or allow-listed identifiers.

## Examples
Flag:
```ts
db.query(`SELECT * FROM users WHERE id = ${req.params.id}`);
```
OK:
```ts
db.query('SELECT * FROM users WHERE id = $1', [req.params.id]);
```

## Implementation notes
Anchor to the query call. Equivalent: `sonarjs/sql-queries`, `security/detect-sql-injection`.

## Required tests
Interpolated value (finding), parameterized (no finding), dynamic ORDER BY without allow-list (finding), allow-listed column (no finding).

## AI prompt
"Is the interpolated value attacker-controlled, and is an allow-list or sanitizer applied?" Evidence: query code, value origin.

## Why hybrid
String-built SQL is syntactic; taint and allow-listing need data flow.
