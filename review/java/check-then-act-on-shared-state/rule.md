# java/check-then-act-on-shared-state: Non-atomic check-then-act on shared map or lazy field

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Concurrency; java-bug-hunter: Concurrency and asynchronous work |

## Intent
A check followed by a separate write is not atomic, even on a `ConcurrentHashMap`. Competing threads can both pass the check and violate an invariant (duplicate creation, lost update, double initialisation).

## Detect
- `if (!map.containsKey(k)) map.put(k, v)` or `if (map.get(k) == null) map.put(k, ...)` on a field of a concurrent or shared map type.
- `if (field == null) field = new ...` lazy initialisation of a shared non-volatile field outside `synchronized`.
- Read-then-write on an `AtomicX` (`if (a.get() < max) a.incrementAndGet()`).

## Do not flag
- `putIfAbsent`, `computeIfAbsent`, `compute`, `merge`, `compareAndSet`.
- The sequence inside a `synchronized` block or lock guarding all accessors.
- Local or thread-confined maps.

## Examples
Flag:
```java
if (!cache.containsKey(id)) { cache.put(id, load(id)); }
```
OK:
```java
cache.computeIfAbsent(id, this::load);
```

## Implementation notes
AST, same-block pattern. Anchor to the `if`.

Detected on an `if` whose condition reads a field of the class (not a local or a parameter), outside `synchronized`: `!m.containsKey(k)` or `m.get(k) == null` followed by `m.put`, `if (f == null) f = ...` on a non-volatile field, and `if (a.get() < n) a.incrementAndGet()`. Only these text shapes are matched, and a field guarded by a `Lock` is not recognised.

## Required tests
containsKey/put (candidate), computeIfAbsent (none), inside synchronized (none), local HashMap (none).

## Why hybrid
Pattern is detectable; whether racing threads can break an invariant depends on usage.

## AI prompt
"Can two threads interleave between the check and the act and violate an invariant (duplicate side effect, lost value)? Report only with the concrete interleaving." Evidence: the sequence, field declaration, callers/thread model.
