# python/boto3-pagination-timeouts-lifecycle: Unpaginated listings, unobserved failures, missing timeouts and client lifecycle

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
List/scan calls use only the first page; boto3/botocore calls lack timeout/retry configuration; clients/sessions are created per call, or shared across threads/forks incorrectly. Blocking boto3 calls inside `async def` are reported by `blocking-call-in-async`.

## Detect
Candidates:
- `list_objects(_v2)`, `scan`, `query`, `list_*`, `describe_*` called without a paginator or `NextToken`/`ContinuationToken`/`LastEvaluatedKey` loop, with the result treated as complete;
- `boto3.client(...)`/`resource(...)` created inside a loop or request handler;
- `boto3.client(...)` without `Config(connect_timeout=, read_timeout=, retries=)` in code on a request path;
- `boto3.Session`/resource objects shared with `threading.Thread`/`multiprocessing`.

## Do not flag
- Paginators, token loops, `MaxKeys` where the first page is intended; Lambda init-time client creation; clients (not resources/sessions) shared across threads.

## Examples
Flag:
```python
keys = [o["Key"] for o in s3.list_objects_v2(Bucket=b)["Contents"]]
```
OK:
```python
keys = [o["Key"] for p in s3.get_paginator("list_objects_v2").paginate(Bucket=b) for o in p.get("Contents", [])]
```

## Implementation notes
Python `ast`; anchor to the call.

## AI prompt
Question: "Does the caller need all results, and can the result exceed one page; does the missing timeout/retry config or client sharing cause a demonstrated failure on this path?" Supply the call, expected sizes, concurrency model. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Call shapes are deterministic; completeness needs and actual thread/fork usage are contextual.
