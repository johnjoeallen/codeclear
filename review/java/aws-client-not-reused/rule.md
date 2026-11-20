# java/aws-client-not-reused: AWS client created per call or never closed

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | PERFORMANCE |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-bug-hunter: AWS SDK |

## Intent
AWS SDK clients own HTTP connection pools and threads. Building one per request is slow and leaks resources unless closed; they should be created once and reused.

## Detect
A call to `<Service>Client.create()` or `<Service>Client.builder()...build()` (including `AmazonS3ClientBuilder.standard().build()`) inside a non-constructor, non-`@Bean`, non-`@PostConstruct` method, assigned to a local that is not in try-with-resources and is not stored in a field.

## Do not flag
- Clients created in `@Bean` methods, constructors, static initialisers or `@PostConstruct`.
- Locals declared in try-with-resources (still flagged only when inside a loop or per-request method, confidence LOW).
- Test sources.

## Examples
Flag:
```java
void upload(byte[] b) { S3Client s3 = S3Client.create(); s3.putObject(req, RequestBody.fromBytes(b)); }
```
OK: `private final S3Client s3;` injected and reused.

## Implementation notes
AST with type resolution. Anchor to the creation expression.

## Required tests
Per-call `create()` (finding), `@Bean` method (none), field initialisation (none), try-with-resources in a loop (finding, LOW).

## Why deterministic
Client type, enclosing method kind and assignment target are syntactic facts.
