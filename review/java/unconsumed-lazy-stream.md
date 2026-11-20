# java/unconsumed-lazy-stream: Stream pipeline never consumed or with lazy side effects

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-bug-hunter: Exceptions, resources and control flow |

## Intent
Stream intermediate operations are lazy. A pipeline without a terminal operation never runs, and side effects in `map`/`peek` may not execute when a short-circuiting terminal operation is used.

## Detect
- An expression statement that ends in an intermediate stream operation (`map`, `filter`, `peek`, `sorted`, `flatMap`, `distinct`, `limit`) with no terminal operation.
- A `Stream` local that is created and never consumed.
- `peek(...)` or `map(...)` containing a side effect (assignment, `add`, `save`, `send`) followed by `count()`/`findFirst()`/`anyMatch()` (which can skip the lambda).

## Do not flag
- Streams returned or passed on to the caller.
- Intermediate operations on `Optional` or reactive `Flux`/`Mono` (use a reactive rule).
- `peek` for logging only.

## Examples
Flag:
```java
items.stream().map(this::save);
```
OK:
```java
items.forEach(this::save);
```

## Implementation notes
AST with type resolution (`java.util.stream.*Stream`). Anchor to the pipeline. Equivalent: Sonar S3958.

## Required tests
No terminal op (finding), `.collect(...)` (none), returned stream (none), `peek` with `add` before `count` (finding), logging `peek` (none).

## Why deterministic
The terminal/intermediate classification of stream operations is a closed set.
