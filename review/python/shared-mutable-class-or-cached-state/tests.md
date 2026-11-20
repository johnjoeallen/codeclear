# Tests: python/shared-mutable-class-or-cached-state

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a list in the class body that a method appends to (Examples: Flag)

covers: example:flag,required:class-list

```python path=app/cart.py
class Cart:
    items = []  # CANDIDATE
    def add(self, item):
        self.items.append(item)
```

## Case: created in __init__, and never changed (Examples: OK)

covers: example:ok,do-not-flag:init-and-readonly,required:init

```python path=app/cart.py
class Cart:
    DEFAULTS = {"a": 1}
    tags = []
    def __init__(self):
        self.items = []
    def add(self, item):
        self.items.append(item)
    def first(self):
        return self.tags[0]
```

## Case: dict, set and item assignment

covers: detect:other-containers

```python path=app/cart.py
class Registry:
    handlers = {}  # CANDIDATE
    seen = set()  # CANDIDATE
    def register(self, k, f):
        self.handlers[k] = f
        self.seen.add(k)
```

## Case: a parameter kept and changed

covers: detect:parameter

```python path=app/cart.py
class Cart:
    def __init__(self, items):
        self.items = items  # CANDIDATE
    def add(self, item):
        self.items.append(item)

class Copy:
    def __init__(self, items):
        self.items = list(items)
    def add(self, item):
        self.items.append(item)
```

## Case: cached functions

covers: detect:cache,do-not-flag:immutable-return

```python path=app/cache.py
@lru_cache(maxsize=None)
def settings():  # CANDIDATE
    return {"a": 1}

@functools.cache
def names():
    return ("a", "b")
```
