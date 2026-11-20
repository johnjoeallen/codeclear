# Tests: python/is-literal-comparison

A `# FLAG` marker ends each line that must produce a finding.

## Case: is with a string (Examples: Flag)

covers: example:flag,required:string

```python path=app/status.py
if status is "ready":  # FLAG
    pass
```

## Case: == (Examples: OK)

covers: example:ok,required:equality

```python path=app/status.py
if status == "ready":
    pass
```

## Case: the other literals and is not

covers: detect:literals

```python path=app/status.py
a = x is 3  # FLAG
b = x is not 1.5  # FLAG
c = x is (1, 2)  # FLAG
d = x is b"ab"  # FLAG
e = "a" "b" is x  # FLAG
```

## Case: None, True, False and Ellipsis (Do not flag)

covers: do-not-flag:singletons,required:none

```python path=app/status.py
a = x is None
b = x is not None
c = x is True
d = x is False
e = x is Ellipsis
f = x is ...
```

## Case: a sentinel created in the module (Do not flag)

covers: do-not-flag:sentinel

```python path=app/status.py
_MISSING = object()
a = x is _MISSING
```

## Case: == with a literal and is between names

covers: detect:is-only

```python path=app/status.py
a = x == "s"
b = x is y
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```python path=tests/test_status.py
assert x is "s"
```
