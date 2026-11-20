# typescript/s3-stream-pagination-multipart: S3 streaming, pagination, multipart cleanup, keys and presigned URLs

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless; node-bug-hunter: AWS and serverless failure windows |

## Intent
`GetObject` bodies are streams that can fail mid-download; multipart uploads that fail must be aborted or they leave billed parts; list results are paginated; object keys from users need validation; presigned URLs must expire appropriately.

## Why AI
Impact depends on object sizes, the completeness promised to the caller and the exposure of the URL.

## AI prompt
"Check: stream error handling and cleanup of both ends; `AbortMultipartUpload` on failure; continuation token handling when the caller promises a complete list; user-controlled keys (traversal, prefix escape); presigned URL expiry/permissions. Name the concrete failure." Evidence: S3 calls, caller promise, key origin, expiry value.

## Do not flag
- Managed uploads (`@aws-sdk/lib-storage` `Upload`) that abort automatically; small bodies read fully.

## Examples
Flag:
```ts
const url = await getSignedUrl(s3, new GetObjectCommand({ Bucket, Key: req.query.key }), { expiresIn: 7 * 86400 });
```
OK: key checked against the user's prefix and short expiry.

## Evaluation cases
1. Key from query used without prefix check: flag.
2. Multipart upload without abort in the catch: flag.
3. Paginator used to build a complete list: ok.
