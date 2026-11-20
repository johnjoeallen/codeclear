# java/missing-network-timeout: Network or database call without bounded timeout

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Resources and I/O; java-bug-hunter: Spring and distributed-service behavior |

## Intent
A stalled dependency without a connect/read timeout blocks the calling thread indefinitely, exhausting thread or connection pools.

## Detect
Construction of an HTTP/socket/DB client with no timeout configured in the same class or factory:
`new RestTemplate()`, `HttpClient.newHttpClient()` / `newBuilder()` without `connectTimeout`, request builders without `.timeout(...)`, `new Socket(host, port)` without `connect(addr, timeout)`/`setSoTimeout`, `URL.openConnection()` without `setConnectTimeout/setReadTimeout`, `WebClient` without response timeout, `Future.get()` and `Condition.await()` without a timeout.

## Do not flag
- Clients built by a shared factory or `RestTemplateBuilder` that sets timeouts.
- Timeouts shown set via config properties or a bean in the diff.
- Test sources.

## Examples
Flag:
```java
RestTemplate rt = new RestTemplate();
```
OK:
```java
RestTemplate rt = builder.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(5)).build();
```

## Implementation notes
AST/regex candidates. Anchor to the construction or call.

Detected on `new RestTemplate()`, `HttpClient.newHttpClient()`, `HttpClient.newBuilder()` without `connectTimeout` in its statement, `new Socket(host, port)`, `openConnection()`, `WebClient.create/builder`, and `Future.get()`/`Condition.await()` with no arguments. Any timeout setting (`setConnectTimeout`, `connectTimeout`, `responseTimeout`, `RestTemplateBuilder`, ...) in the same class counts as configured, which is how a shared factory is recognised.

## Required tests
`new RestTemplate()` (candidate), builder with timeouts (none), `HttpRequest` with `.timeout` (none), `future.get()` (candidate).

## Why hybrid
A timeout may be configured elsewhere (properties, interceptors, shared beans).

## AI prompt
"Is a connect and read timeout configured for this client anywhere in the shown context? Report only if none is, and describe what blocks." Evidence: client creation, related configuration classes and application properties.
