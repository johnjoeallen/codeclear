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
type is `Optional`, `OptionalInt`, `OptionalLong` or `OptionalDouble`, when no dominating guard on that same variable appears earlier in the same method. A guard is an
`isPresent()` / `isEmpty()` check (if, else, ternary, `&&`/`||`, or an earlier early-exit `if`), an `assert` or an earlier `assertThat(x).isPresent()` / `isNotEmpty()` statement,
or an enclosing `ifPresent` / `ifPresentOrElse` lambda. When the variable is a method parameter, the callers in the project are followed (a few hops): the parameter is guarded when the method has callers and every one passes `Optional.of(...)` or a variable that is guarded at the call, or is itself a parameter whose callers all guard it. `orElse*` is not a guard: it does not prove presence.

## Do not flag
- Variable guarded by `if (opt.isPresent())` or `if (opt.isEmpty()) return/throw` in the same scope.
- A parameter that every caller in the project guards or passes as `Optional.of(...)`.
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
`confidence: LOW`. When the receiver's type cannot be resolved, treat it as an Optional if it is a call to one
of `findById`, `findFirst`, `findAny`, `findOne`, `max`, `min`, `reduce`, `findFirstBy`, `findOneBy`, or a
variable whose declared type is `Optional`, `OptionalInt`, `OptionalLong` or `OptionalDouble`. Equivalent: Sonar `S3655`, SpotBugs `NP_OPTIONAL_RETURN_NULL`.

## Required tests
Chained `findById(..).get()` (finding), guarded by `isPresent` (no finding), `Future.get()` (no
finding), `orElseThrow` (no finding).
