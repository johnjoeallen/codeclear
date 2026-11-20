# Tests: python/return-in-finally

A `# FLAG` marker ends each line that must produce a finding.

## Case: a return in finally (Examples: Flag)

covers: example:flag,required:return

```python path=app/work.py
def f():
    try:
        work()
    finally:
        return result  # FLAG
```

## Case: cleanup only (Examples: OK)

covers: example:ok,required:cleanup

```python path=app/work.py
def f():
    try:
        work()
    finally:
        cleanup()
```

## Case: break and continue

covers: required:break-continue,detect:loop-outside

```python path=app/work.py
def f(items):
    for i in items:
        try:
            work(i)
        finally:
            break  # FLAG
    for i in items:
        try:
            work(i)
        finally:
            continue  # FLAG
```

## Case: a loop inside the finally

covers: required:loop-inside

```python path=app/work.py
def f(items):
    try:
        work()
    finally:
        for i in items:
            if i:
                break
            continue
```

## Case: a return in a function or lambda defined in the finally (Do not flag)

covers: do-not-flag:nested-function,required:nested

```python path=app/work.py
def f():
    try:
        work()
    finally:
        def g():
            return 1
        h = lambda: 2
        cleanup(g, h)
```

## Case: a return in the try or except, and in a nested finally

covers: detect:position

```python path=app/work.py
def f():
    try:
        return work()
    except ValueError:
        return None
    finally:
        try:
            cleanup()
        finally:
            return 0  # FLAG
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_work.py
def f():
    try:
        work()
    finally:
        return 1
```
