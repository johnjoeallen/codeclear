# python/sql-string-formatting: SQL built with string formatting

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | python-review: Databases, ORM, and transactions |

## Intent
Interpolating values into SQL text allows SQL injection. Bound parameters keep values out of the query.

## Detect
An argument to `.execute`, `.executemany`, `.raw`, `text(...)`, or `cursor.execute` that is an f-string with substitutions, a `%` format, `.format(...)`, or `+` concatenation involving a non-literal, and whose literal part begins with `SELECT`, `INSERT`, `UPDATE`, `DELETE` (case-insensitive).

## Do not flag
- Constant strings, and placeholders with a separate parameters argument (`%s`, `?`, `:name`).
- Interpolating a table or column name taken from a hard-coded allowlist in the same function.

## Examples
Flag:
```python
cur.execute(f"SELECT * FROM users WHERE id = {user_id}")
```
OK:
```python
cur.execute("SELECT * FROM users WHERE id = %s", (user_id,))
```

## Implementation notes
Python `ast`. Equivalent: Bandit `B608`, Ruff `S608`.

## Why deterministic
A formatted/concatenated string passed to an `execute`-style call is a syntactic fact.

Detected on `execute`, `executemany`, `raw` and `text(...)` calls whose first argument starts like a statement (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) and is an f-string with a substitution, a `%` expression, a `.format()` call, or a `+` concatenation that contains a name. An allow-listed table name in the same function is not recognised, so it is reported.

## Required tests
f-string (finding), `%` format (finding), parameterized (no finding), constant query (no finding).
