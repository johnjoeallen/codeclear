# Tests: python/cache-nonatomic-read-modify-write

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: get then set (Examples: Flag)

covers: example:flag,required:get-set

```python path=app/counter.py
import redis
def bump(r, k):
    n = int(r.get(k) or 0)
    r.set(k, n + 1)  # CANDIDATE
```

## Case: INCR (Examples: OK)

covers: example:ok,do-not-flag:atomic-forms,required:incr

```python path=app/counter.py
import redis
def a(r, k):
    r.incr(k)

def b(r, k):
    with r.pipeline() as p:
        p.watch(k)
        n = int(p.get(k) or 0)
        p.multi()
        p.set(k, n + 1)
        p.execute()
```

## Case: set then expire

covers: detect:set-expire

```python path=app/counter.py
import redis
def a(r, k, v):
    r.set(k, v)
    r.expire(k, 60)  # CANDIDATE

def b(r, k, v):
    r.set(k, v, ex=60)
```

## Case: a get without a set, and a different key

covers: detect:other-keys

```python path=app/counter.py
import redis
def a(r, k):
    return r.get(k)

def b(r, k, j):
    n = r.get(k)
    r.set(j, 1)
```

## Case: set with nx

covers: detect:nx

```python path=app/counter.py
import redis
def a(r, k):
    if r.get(k) is None:
        r.set(k, 1, nx=True, ex=10)
```
