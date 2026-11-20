# java/aws-hardcoded-credentials-or-region: Hard-coded AWS credentials, region or endpoint

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | HIGH for credentials, LOW for region |
| Detection type | hybrid |
| Source | java-bug-hunter: AWS SDK |

## Intent
Static credentials leak secrets and cannot rotate; a literal region or endpoint breaks portability between environments. The default provider chains avoid both.

## Detect
- `AwsBasicCredentials.create("...", "...")`, `new BasicAWSCredentials(...)`, `StaticCredentialsProvider` with literal arguments.
- `.region(Region.of("..."))`/`Region.US_EAST_1`/`.withRegion("...")` or `.endpointOverride(URI.create("https://..."))` with a literal.

## Do not flag
- Credentials read from config/env and passed in; `DefaultCredentialsProvider`; `ProfileCredentialsProvider`.
- Test sources and LocalStack endpoint overrides in test configuration.
- Region literal where the service is global or single-region by design (report credentials only).

## Examples
Flag:
```java
S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIA...", "wJal..."))).build();
```
OK: `S3Client.create()` using the default chains.

## Implementation notes
AST. Anchor to the literal; never echo the secret. Credentials are a secret finding regardless of deployment; region/endpoint only with evidence of a deployment issue.

## Required tests
Literal access key (finding), `DefaultCredentialsProvider` (none), literal region (candidate), test source (none).

## Why hybrid
Credential literals are certain, but a hard-coded region is a defect only if environments differ.

## AI prompt
"Does the hard-coded region/endpoint break a deployment or environment shown in the context, or expose a secret?" Evidence: the literal, deployment/profile config, other environments' settings.
