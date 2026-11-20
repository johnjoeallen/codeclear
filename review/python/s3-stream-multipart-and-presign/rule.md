# python/s3-stream-multipart-and-presign: S3 stream closure, multipart cleanup, key validation and presigned expiry

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
`get_object()["Body"]` not closed or read partially; multipart uploads not aborted on failure (orphaned parts cost money); partial read failures treated as complete; user-supplied keys not validated; presigned URLs with excessive or too short expiry.

## Why AI
Needs ownership of the streaming body, the caller's completeness needs, lifecycle rules and the trust level of the key.

## AI prompt
"Trace the S3 stream/multipart upload through read/upload failure: is the body closed, is the upload aborted, and does a truncated read look like success? Is a user-controlled key validated/prefixed? Is the presigned URL expiry appropriate for the use?" Supply the code and key/expiry sources. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- `with` / `try/finally` closure; bucket lifecycle rule for incomplete multipart uploads shown; `upload_file` managed transfer.

## Examples
Flag:
```python
mpu = s3.create_multipart_upload(Bucket=b, Key=k)
for part in parts: s3.upload_part(...)           # failure leaves orphaned upload
```
OK:
```python
try: ... complete_multipart_upload(...)
except Exception: s3.abort_multipart_upload(Bucket=b, Key=k, UploadId=mpu["UploadId"]); raise
```

## Evaluation cases
1. Above flag example -> flag.
2. `body = s3.get_object(...)["Body"]; return body` without closing in a request-scoped handler -> flag.
3. `Key=request.args["key"]` for a private bucket with no prefix check -> flag.
4. `upload_file(...)` -> no finding.
