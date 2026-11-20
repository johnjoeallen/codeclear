# python/mutable-default-argument: Mutable default argument

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Values, state, and control flow |

## Intent
Default values are evaluated once. A mutable default is shared by every call, so one call changes the next one's state.

## Detect
A function or lambda parameter default that is a list, dict or set literal or comprehension, or a call to `list()`, `dict()`, `set()`, `defaultdict()`, `deque()`, `bytearray()`.

## Do not flag
- `None`, tuples, `frozenset`, strings, numbers.
- Dataclass `field(default_factory=...)`.
- Defaults that are never mutated and the function is private with a comment: use `confidence: LOW` only.

## Examples
Flag:
```python
def add(item, items=[]):
    items.append(item)
```
OK:
```python
def add(item, items=None):
    items = [] if items is None else items
```

## Implementation notes
Python `ast` module. Equivalent: Ruff `B006`.

## Why deterministic
A mutable literal or constructor call in a parameter default is a syntactic fact.

Detected on `default_parameter` and `typed_default_parameter` of functions and lambdas. A dataclass `field(default_factory=...)` is a class attribute, not a parameter, so it is never seen. The LOW-confidence case for private, never-mutated functions is not detected.

## Required tests
List, dict, set, `list()` call defaults (findings); `None`, tuple (no finding); keyword-only default; lambda default.
