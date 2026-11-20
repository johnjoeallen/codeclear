# Tests: python/naive-datetime

A `# FLAG` marker ends each line that must produce a finding.

## Case: utcnow (Examples: Flag)

covers: example:flag,required:utcnow

```python path=app/clock.py
created = datetime.utcnow()  # FLAG
```

## Case: now with a timezone (Examples: OK)

covers: example:ok,do-not-flag:tz,required:tz

```python path=app/clock.py
a = datetime.now(timezone.utc)
b = datetime.now(tz=timezone.utc)
c = datetime.datetime.now(tz=ZoneInfo("UTC"))
d = datetime.fromtimestamp(ts, timezone.utc)
e = datetime.fromtimestamp(ts, tz=timezone.utc)
```

## Case: now and fromtimestamp without a timezone

covers: detect:no-tz

```python path=app/clock.py
a = datetime.now()  # FLAG
b = datetime.datetime.now()  # FLAG
c = datetime.fromtimestamp(ts)  # FLAG
d = datetime.utcfromtimestamp(ts)  # FLAG
```

## Case: other receivers

covers: detect:receiver

```python path=app/clock.py
a = arrow.utcnow()
b = clock.now()
c = date.today()
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```python path=tests/test_clock.py
a = datetime.utcnow()
b = datetime.now()
```
