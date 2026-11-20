# Tests: python/sql-string-formatting

A `# FLAG` marker ends each line that must produce a finding.

## Case: an f-string (Examples: Flag)

covers: example:flag,required:f-string

```python path=app/db.py
cur.execute(f"SELECT * FROM users WHERE id = {user_id}")  # FLAG
```

## Case: parameters (Examples: OK)

covers: example:ok,do-not-flag:parameters,required:parameters

```python path=app/db.py
cur.execute("SELECT * FROM users WHERE id = %s", (user_id,))
cur.execute("SELECT * FROM users WHERE id = ?", (user_id,))
cur.execute("SELECT * FROM users WHERE id = :id", {"id": user_id})
```

## Case: %, .format and concatenation

covers: detect:forms,required:percent

```python path=app/db.py
cur.execute("SELECT * FROM t WHERE id = %s" % uid)  # FLAG
cur.execute("select " + col + " from t")  # FLAG
cur.execute("SELECT {}".format(x))  # FLAG
cur.execute("DELETE FROM t WHERE id = " + str(i))  # FLAG
```

## Case: the other calls and statements

covers: detect:calls

```python path=app/db.py
cur.executemany(f"INSERT INTO t VALUES ({v})", rows)  # FLAG
qs = Model.objects.raw(f"UPDATE t SET a = {a}")  # FLAG
stmt = text(f"SELECT * FROM t WHERE a = {a}")  # FLAG
```

## Case: constant strings and strings that are not statements (Do not flag)

covers: do-not-flag:constant

```python path=app/db.py
cur.execute("SELECT * FROM users")
cur.execute("SELECT " "1")
log.execute(f"hello {name}")
cur.execute(f"VACUUM {name}")
cur.execute(query)
```

## Case: an allow-listed table name is still reported

covers: do-not-flag:allowlist

```python path=app/db.py
def f(table):
    assert table in ALLOWED
    cur.execute(f"SELECT * FROM {table}")  # FLAG
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_db.py
cur.execute(f"SELECT * FROM t WHERE a = {a}")
```
