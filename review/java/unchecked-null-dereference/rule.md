# java/unchecked-null-dereference: Possibly null value dereferenced without a check

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Nulls and values; java-bug-hunter: Exceptions, resources and control flow |

## Intent
Values from `Map.get`, nullable repository results and optional JSON/external fields can be null; dereferencing them unguarded throws `NullPointerException`.

## Detect
A method call or field access directly on the result of `Map.get(...)`, a repository/DAO method returning a nullable type (`findBy...` not returning `Optional`), `JsonNode.get(...)`, or an external payload getter, or on a local assigned from one, with no null check, `Objects.requireNonNull`, `getOrDefault` or `computeIfAbsent` before use.

## Do not flag
- `Optional`-returning methods (see `optional-get-without-check`).
- Maps known to contain the key (immediately preceded by `containsKey` or `put` on the same key).
- `@NonNull`-annotated return types.

## Examples
Flag:
```java
String name = users.get(id).getName();
```
OK:
```java
User u = users.get(id);
if (u == null) throw new UserNotFoundException(id);
```

## Implementation notes
AST with symbol resolution. Anchor to the dereference.

Detected on a call made directly on `Map.get(k)`, `JsonNode.get(name)`, or a `find/get/load/lookupBy...` method of a repository or DAO whose return type in the project is not `Optional` or a collection. A method that contains `containsKey`, `requireNonNull`, `getOrDefault`, `computeIfAbsent`, a null check or a `put` is skipped as a whole; a local assigned from such a call and used later is not followed.

## Required tests
Direct chain on `Map.get` (candidate), checked local (none), `getOrDefault` (none), Optional return (none).

## Why hybrid
Whether the value can really be null at that point depends on data invariants outside the method.

## AI prompt
"Can this value be null or absent at runtime given the producer shown? Report only with a concrete input that causes the NPE." Evidence: the dereference, the producing method or repository signature, and any validation upstream.
