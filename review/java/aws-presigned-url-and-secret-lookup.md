# java/aws-presigned-url-and-secret-lookup: Presigned URL lifetime or per-call secret/config lookup

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: AWS SDK |

## Intent
A presigned URL that expires before the client uses it (latency, clock skew), or a Secrets Manager/SSM lookup on every request that hits rate limits and adds latency, causes runtime failures.

## Why AI
It only matters when the changed flow shows a concrete expiry, skew, latency or throttling failure; the right lifetime and cache policy depend on the use case.

## AI prompt
"For presigned URLs, compare the expiry with the time between creation and use in the flow shown. For secret/parameter lookups, check whether they run per request without caching. Report only with the demonstrated failure (expired URL, throttling at a stated rate)." Evidence: presign call and its consumer, lookup call site, request rate, caching.

## Do not flag
- Lifetimes comfortably longer than the shown usage window.
- Lookups cached or performed at startup.

## Examples
Flag: `presign(Duration.ofSeconds(10))` returned to a mobile client that uploads large files.
OK: 15-minute expiry for an immediate download link; secret cached via a bean.

## Evaluation cases
1. 10s expiry URL handed to slow client uploads - report.
2. `getSecretValue` inside a controller method at high traffic, uncached - report.
3. Secret fetched once in `@PostConstruct` - no report.
4. Expiry 1h for immediate download - no report.
