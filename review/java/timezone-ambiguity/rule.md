# java/timezone-ambiguity: Timestamps crossing boundaries without zone information

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Serialization, JPA and data contracts |

## Intent
`LocalDateTime`, `new Date()` formatted in the default zone, or `ZoneId.systemDefault()` carry no zone; across services, regions or DST they are interpreted differently and give wrong instants.

## Detect
- A `LocalDateTime`/`LocalDate` field in a DTO/event/entity exposed in an API, message or persisted column shared across services.
- `ZoneId.systemDefault()`, `TimeZone.getDefault()`, `LocalDateTime.now()` (no `Clock`/zone), `atZone(ZoneId.systemDefault())`, `new SimpleDateFormat` without `setTimeZone`.

## Do not flag
- Wall-clock concepts (opening hours, birthdays, local appointment times) where local semantics are intended.
- Values converted explicitly with a fixed zone (`ZoneOffset.UTC`).
- Test sources.

## Examples
Flag:
```java
record OrderEvent(String id, LocalDateTime createdAt) {}
```
OK: `Instant createdAt` or `OffsetDateTime` in UTC.

## Implementation notes
AST with type resolution. Anchor to the field or call.

## Required tests
`LocalDateTime` in event DTO (candidate), `Instant` (none), `systemDefault()` (candidate), `ZoneOffset.UTC` (none).

## Why hybrid
Whether the value is intended as wall-clock or crosses regions needs domain knowledge.

## AI prompt
"Does this timestamp cross a service, region or DST boundary where the zone is needed? Report only with the concrete misinterpretation." Evidence: the type, producers/consumers, DB column type, domain meaning.
