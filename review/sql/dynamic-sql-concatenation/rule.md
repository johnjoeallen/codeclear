# sql/dynamic-sql-concatenation: Dynamic SQL built from concatenated values

| Field | Value |
|---|---|
| Applies to | `**/*.sql` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | sql-migration-review: untrusted values interpolated into queries |

## Intent
Stored procedures and scripts that concatenate parameters into a command string and execute it are open to SQL injection.

## Detect
`EXEC('...' + @var ...)`, `EXECUTE(@sql)` where `@sql` is assembled with `+` from a parameter, `EXECUTE IMMEDIATE ... || var`, and PL/pgSQL `EXECUTE 'text' || var` without `format('%I'/'%L')` or `USING`.

## Do not flag
- `sp_executesql` with a parameter definition and parameters.
- `EXECUTE ... USING`.
- `format()` with `%I` for identifiers and `%L` for literals.
- Variables built only from constants.

## Examples
Flag:
```sql
EXEC('SELECT * FROM users WHERE name = ''' + @name + '''');
```
OK:
```sql
EXEC sp_executesql N'SELECT * FROM users WHERE name = @n', N'@n nvarchar(100)', @n = @name;
```

## Implementation notes
Rules are dialect specific: implement per dialect and detect the dialect from syntax markers (`@var`, `$$`).

## Required tests
Concatenated `EXEC` (finding), `sp_executesql` with parameters (no finding), PL/pgSQL `format('%L')` (no finding), `EXECUTE ... USING` (no finding).

## Why deterministic
Concatenating a variable into an executed string, without a binding or quoting construct, is visible in the syntax. Wrong-row query shapes are covered separately by `sql/query-returns-wrong-rows`.
