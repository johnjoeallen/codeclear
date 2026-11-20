# java/shared-unsafe-collection: HashMap or ArrayList shared across threads

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Concurrency; java-bug-hunter: Concurrency and asynchronous work |

## Intent
`HashMap`, `ArrayList` and similar are not thread-safe; concurrent mutation can lose updates, loop forever or throw `ConcurrentModificationException`.

## Detect
A `static` field, or a field of a singleton Spring bean, whose type or initializer is `HashMap`, `LinkedHashMap`, `TreeMap`, `HashSet`, `ArrayList`, `LinkedList` or `ArrayDeque`, and that is mutated (`put`, `add`, `remove`, `clear`) in a non-constructor method, or is captured by a lambda submitted to an executor/`parallelStream`.

## Do not flag
- Fields wrapped with `Collections.synchronizedX`, `ConcurrentHashMap`, `CopyOnWriteArrayList`.
- Mutations guarded by `synchronized` or a `Lock` on every access.
- Collections populated only in the constructor/`@PostConstruct` and never mutated afterward.

## Examples
Flag:
```java
private final Map<String, Session> sessions = new HashMap<>();
void open(String id) { sessions.put(id, new Session()); }
```
OK: `new ConcurrentHashMap<>()`.

## Implementation notes
AST. Anchor to the field declaration.

A lambda handed to an executor or `parallelStream`, and mutations guarded by a `Lock` rather than `synchronized`, are not detected: a field changed only inside `synchronized` code is skipped, and one changed under a `Lock` is left to the AI.

## Required tests
Mutated `HashMap` in `@Component` (candidate), `ConcurrentHashMap` (none), read-only after `@PostConstruct` (none), synchronized accessors (none).

## Why hybrid
Detection is structural; concurrent access needs call-path context.

## AI prompt
"Is this collection accessed by multiple threads without synchronisation? Report only with a concrete concurrent access path." Evidence: field, all accessors, bean scope, executor usage.
