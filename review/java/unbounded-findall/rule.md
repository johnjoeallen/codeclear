# java/unbounded-findall: Unbounded repository query or whole-file read

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | PERFORMANCE |
| Severity | LOW |
| Confidence | LOW |
| Detection type | hybrid |
| Source | java-review: Resources and I/O |

## Intent
Loading every row or whole file into memory can exhaust the heap as the data grows.

## Detect
- Call to `findAll()` with no arguments on a Spring Data repository (a field whose type ends in
  `Repository`).
- `Files.readAllBytes`, `Files.readString`, `Files.readAllLines`, or `InputStream.readAllBytes()`
  on a path or stream that is not a classpath resource or a known small configuration file.

## Do not flag
- `findAll(Pageable)` or `findAll(Specification, Pageable)`.
- Test sources, and repositories of reference/enum tables (name ends in `Type`, `Status`, `Config`).
- Reads of classpath resources.

## Examples
Flag:
```java
List<Order> all = orderRepository.findAll();
```
OK:
```java
Page<Order> page = orderRepository.findAll(PageRequest.of(0, 100));
```

## Implementation notes
Candidate detection is syntactic. Table size is not knowable, so use `hybrid` and
`confidence: LOW`; the AI reviewer decides whether the context makes it a real risk.

## AI prompt
For each candidate call: is the table or file plausibly large or growing, given entity names, usage and any size evidence in the MR? Report only when unbounded loading can cause memory exhaustion in a shown path. Evidence: the call site, the entity or file source, any visible size or retention information.

## Why hybrid
The call is syntactic, but data volume is unknowable from code alone.

## Required tests
`findAll()` (finding), `findAll(pageable)` (no finding), `Files.readString` on user path (finding),
classpath resource (no finding).
