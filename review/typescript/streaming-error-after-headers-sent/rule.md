# typescript/streaming-error-after-headers-sent: Error handling after streaming has begun

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
Once headers or part of the body are sent, an error cannot change the status. Code that sets a 500 or JSON error after streaming starts throws, or the client gets a truncated body that looks successful.

## Why AI
Depends on when the first byte is written, the stream pipeline and the client's expectation (e.g. content-length, chunked).

## AI prompt
"At which point do headers get sent? If an error occurs afterwards, what does the client see (truncated 200, hang, double response)? Report only with a concrete failing source and client-visible result." Evidence: streaming handler, error middleware, pipeline calls.

## Do not flag
- Handlers that destroy the response/socket on error and signal failure.
- Responses fully built before sending.

## Examples
Flag:
```ts
res.write(head); for await (const r of rows()) res.write(r); // rows throws: client gets truncated 200 as success
```
OK: `pipeline(rows, res, (err) => { if (err) res.destroy(err); })`.

## Evaluation cases
1. Write loop with a catch that calls `res.status(500).json()`: flag.
2. Catch destroys the response: ok.
3. Whole payload computed before `res.json`: ok.
