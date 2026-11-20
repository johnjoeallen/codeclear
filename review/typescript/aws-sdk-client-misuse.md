# typescript/aws-sdk-client-misuse: SDK send failures, retries, pagination and client lifecycle

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless |

## Intent
SDK calls must be awaited and their failures observed, retries must be safe for the operation, list calls must follow continuation tokens, and clients must follow the actual lifecycle (reuse in long-lived processes, cleanup when needed).

## Why AI
Needs the client config (retry strategy), the operation's idempotency and the runtime type. Do not prescribe per-request client construction.

## AI prompt
"Check: unobserved `client.send`; non-idempotent operation under default SDK retries; paginated call (`ListObjectsV2`, `Scan`, `Query`) treated as complete; clients that leak or are recreated per invocation in a long-lived process. Report only with a concrete failure." Evidence: SDK call, client construction, retry config, caller expectations.

## Do not flag
- Paginators (`paginateListObjectsV2`), awaited calls, module-level clients.

## Examples
Flag:
```ts
const out = await s3.send(new ListObjectsV2Command({ Bucket })); return out.Contents; // truncated at 1000
```
OK: `for await (const p of paginateListObjectsV2({ client: s3 }, { Bucket })) ...`

## Evaluation cases
1. Single `Scan` treated as full table: flag.
2. `new S3Client()` inside a hot request handler in a server (not Lambda cold path): flag.
3. Module-level client reused by Lambda: ok.
