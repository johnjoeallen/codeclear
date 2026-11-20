# Tests: python/mutable-default-argument

A `# FLAG` marker ends each line that must produce a finding.

## Case: a list default (Examples: Flag)

covers: example:flag,required:list-default

```python path=app/cart.py
def add(item, items=[]):  # FLAG
    items.append(item)
```

## Case: None and a fresh list inside (Examples: OK)

covers: example:ok,do-not-flag:none,required:none-default

```python path=app/cart.py
def add(item, items=None):
    items = [] if items is None else items
```

## Case: dict, set, comprehensions and calls, in def, lambda and typed parameters

covers: detect:kinds

```python path=app/cart.py
def a(x={}):  # FLAG
    pass
def b(x={1, 2}):  # FLAG
    pass
def c(x=[i for i in range(3)]):  # FLAG
    pass
def d(x=list()):  # FLAG
    pass
def e(x=collections.defaultdict(list)):  # FLAG
    pass
def f(x: list = []):  # FLAG
    pass
g = lambda x=[]: x  # FLAG
def h(x=deque()):  # FLAG
    pass
def i(y=bytearray()):  # FLAG
    pass
```

## Case: immutable defaults (Do not flag)

covers: do-not-flag:immutable,required:tuple

```python path=app/cart.py
def a(x=(), y=frozenset(), z="s", n=1, f=1.5, b=True, t=(1, 2)):
    pass
```

## Case: a dataclass default factory (Do not flag)

covers: do-not-flag:default-factory

```python path=app/cart.py
@dataclass
class Cart:
    items: list = field(default_factory=list)
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```python path=tests/test_cart.py
def add(item, items=[]):
    items.append(item)
```
