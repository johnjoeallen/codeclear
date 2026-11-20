# java/optional-get-without-check: Optional.get() without presence check

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | java-review: Nulls and values; java-bug-hunter: Exceptions, resources and control flow |

## Intent
`Optional.get()` throws `NoSuchElementException` when empty. Repository lookups and map-derived values are commonly empty.

## Detect
A `.get()` call (or `getAsInt()`, `getAsLong()`, `getAsDouble()`) on an expression whose static
type is `Optional`, `OptionalInt`, `OptionalLong` or `OptionalDouble`, when no dominating `isPresent()`, `isEmpty()`, `ifPresent` or `orElse*` guard appears earlier in
the same method on the same variable.

## Do not flag
- Variable guarded by `if (opt.isPresent())` or `if (opt.isEmpty()) return/throw` in the same scope.
- Test sources.
- `.get()` on `Supplier`, `Future`, `AtomicReference` or other non-Optional types.

## Examples
Flag:
```java
User user = repo.findById(id).get();
```
OK:
```java
User user = repo.findById(id).orElseThrow(() -> new NotFoundException(id));
```

## Implementation notes
Needs type resolution (JavaParser symbol solver) or falls back to a naming heuristic with
`confidence: LOW`. Equivalent: Sonar `S3655`, SpotBugs `NP_OPTIONAL_RETURN_NULL`.

## Required tests
Chained `findById(..).get()` (finding), guarded by `isPresent` (no finding), `Future.get()` (no
finding), `orElseThrow` (no finding).
