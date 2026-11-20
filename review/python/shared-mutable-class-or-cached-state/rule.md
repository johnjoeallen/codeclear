# python/shared-mutable-class-or-cached-state: Shared mutable class attribute, cached mutable result or aliased collection

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Values, state, and control flow |

## Intent
A mutable object shared across instances, calls or requests (class attribute, `lru_cache`/`cache` result, module global, aliased list/dict) lets one call change another's state. `mutable-default-argument` covers default arguments only.

## Detect
Candidates:
- class-body assignments of `[]`, `{}`, `set()` or `dict()` that a method mutates through `self.<name>` (append, update, item assignment);
- `@functools.cache`/`@lru_cache` functions returning a list, dict or set;
- a parameter or attribute stored and later mutated without copy (`self.items = items; self.items.append(...)`).

## Do not flag
- Dataclass `field(default_factory=...)`; class attributes only read; `ClassVar` registries deliberately shared and guarded.
- Cached functions returning tuples, frozensets or immutable models.

## Examples
Flag:
```python
class Cart:
    items = []
    def add(self, x): self.items.append(x)
```
OK:
```python
class Cart:
    def __init__(self): self.items = []
```

## Implementation notes
Python `ast` finds candidates; anchor to the mutable assignment or the cached function.

## AI prompt
Question: "Is this mutable object reachable from more than one instance, call or request, and does a visible mutation change another caller's result?" Supply the class or function, the mutating sites, and callers if shown. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The detector finds the shapes; whether sharing is intended (registry, cache of immutable data) depends on usage.
