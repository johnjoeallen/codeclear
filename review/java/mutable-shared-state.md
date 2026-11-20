# java/mutable-shared-state: Mutable static field or singleton bean state

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Concurrency; java-bug-hunter: Concurrency and asynchronous work |

## Intent
Mutable `static` fields and instance fields of singleton Spring beans are shared by all request/worker threads; concurrent writes corrupt or leak state between requests.

## Detect
- A non-final `static` field, or a `static final` field of a mutable type (`List`, `Map`, `Set`, `StringBuilder`, array), that is assigned or modified outside a static initializer.
- A non-final instance field in a `@Component/@Service/@Repository/@Controller/@RestController` class (singleton scope) that is assigned in a non-constructor method.

## Do not flag
- Immutable state, `final` fields initialised once, `Atomic*`, `ConcurrentHashMap`, `volatile` with single-writer use.
- `@Scope("prototype"|"request"|"session")` beans.
- Injected dependencies set by Spring.

## Examples
Flag:
```java
@Service class Cart { private List<Item> items = new ArrayList<>(); void add(Item i) { items.add(i); } }
```
OK: the list as a method-local variable or `@Scope("prototype")`.

## Implementation notes
AST for candidates. Anchor to the field declaration. Thread-unsafe collections are in `shared-unsafe-collection`.

## Required tests
Mutated field in `@Service` (candidate), `final` injected dependency (none), prototype scope (none), `AtomicInteger` (none).

## Why hybrid
Whether concurrent threads actually reach the state depends on scope and call paths.

## AI prompt
"Can two threads concurrently read and write this state in a way that corrupts it or leaks data between requests? Report only with a concrete interleaving." Evidence: field, its writers/readers, bean scope.
