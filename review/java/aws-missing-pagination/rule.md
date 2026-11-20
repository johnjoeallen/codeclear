# java/aws-missing-pagination: Single page used where all results are expected

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: AWS SDK |

## Intent
List/scan/query AWS calls return at most one page (1 MB for DynamoDB, 1000 keys for S3). Reading only the first response silently drops the rest.

## Detect
A call to `listObjects(V2)`, `listObjectsV2`, `scan`, `query`, `listQueues`, `describe*`/`list*` paginated operations whose response is used without a paginator (`*Paginator`, `iterable()`), no loop over `nextContinuationToken`/`lastEvaluatedKey`/`nextToken`, and no `limit`/`maxKeys` indicating a bounded read.

## Do not flag
- Use of SDK paginators.
- Calls with explicit `limit(1)`/`maxKeys(n)` where the code intends a bounded sample.
- Operations that are not paginated.

## Examples
Flag:
```java
return s3.listObjectsV2(req).contents();
```
OK: `s3.listObjectsV2Paginator(req).contents().stream()`.

## Implementation notes
AST with SDK type resolution. Anchor to the call.

Detected on the listed paginated operations (`listObjects(V2)`, `scan`, `query`, `listQueues`, `describe*` and a few `list*`) called on a variable whose declared type is an AWS `*Client`. The method is searched for `nextContinuationToken`, `lastEvaluatedKey`, `nextToken` and similar, and the call for `maxKeys`/`limit(n)`; either skips it. Whether the data can exceed a page is left to the AI.

## Required tests
Single `listObjectsV2` (candidate), paginator (none), manual token loop (none), `maxKeys(1)` (none).

## Why hybrid
Detecting the call is syntactic; whether the code needs all results is intent.

## AI prompt
"Does this code require all matching results? Report only if more results than one page are plausible and the code treats the page as the full set." Evidence: the call, how results are used, expected data volume.
