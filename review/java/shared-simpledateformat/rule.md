# java/shared-simpledateformat: SimpleDateFormat held in a shared field

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Concurrency; java-bug-hunter: Concurrency and asynchronous work |

## Intent
`SimpleDateFormat` and `DateFormat` are not thread-safe. A shared instance can return corrupted dates under concurrent use.

## Detect
A `static` field, or an instance field of a Spring singleton (`@Component`, `@Service`,
`@Repository`, `@Controller`, `@RestController`, `@Configuration`), whose type or initializer is
`SimpleDateFormat` or `DateFormat`.

## Do not flag
- Local variables.
- Fields wrapped in `ThreadLocal`.
- Fields that are never used outside a `synchronized` block and are `private final`: use `confidence: LOW`.

## Examples
Flag:
```java
private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");
```
OK:
```java
private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
```

## Implementation notes
AST on field declarations. Suggest `java.time.DateTimeFormatter` in the message.

## Required tests
Static field (finding), `@Service` instance field (finding), local variable (no finding),
`ThreadLocal<SimpleDateFormat>` (no finding).
