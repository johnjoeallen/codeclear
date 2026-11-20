# java/unclosed-resource: Closeable not closed on every exit path

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Resources and I/O; java-bug-hunter: Exceptions, resources and control flow |

## Intent
Streams, readers, connections, channels and HTTP responses that are not closed on exceptional as well as normal exits leak handles and pool connections.

## Detect
A local variable initialised with `new FileInputStream/FileReader/Socket/...`, `Files.newInputStream/newBufferedReader/lines/walk/list`, `dataSource.getConnection()`, `httpClient.execute(...)` or another `AutoCloseable` creation, that is not declared in try-with-resources and has no `close()` in a `finally`, and is not returned or stored in a field.

## Do not flag
- Resources returned to the caller, stored in a field, or passed to a method that takes ownership.
- Framework-managed resources (Spring `JdbcTemplate`, `@Autowired` clients, `RestTemplate` responses).
- `Closeable` wrappers with no-op close (`ByteArrayInputStream`, `StringReader`).

## Examples
Flag:
```java
InputStream in = new FileInputStream(f);
byte[] b = in.readAllBytes();
```
OK:
```java
try (InputStream in = new FileInputStream(f)) { return in.readAllBytes(); }
```

## Implementation notes
AST with type resolution. Anchor to the creation expression. Equivalent: SpotBugs `OBL_UNSATISFIED_OBLIGATION`, Sonar S2095.

## Required tests
Unclosed stream (candidate), try-with-resources (none), close in finally (none), returned stream (none), `ByteArrayInputStream` (none).

## Why hybrid
Ownership transfer to helpers or frameworks is not always visible.

## AI prompt
"Is this resource closed on every exit path, including exceptions, by this method or an owner it is handed to?" Evidence: the full method and any helper that receives the resource.
