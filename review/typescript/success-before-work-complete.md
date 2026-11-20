# typescript/success-before-work-complete: Response, commit or ack issued before required work finishes

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-bug-hunter: Trace the execution before judging it; Async completion and error ownership |

## Intent
The success boundary (HTTP response, promise return, transaction commit, message ack, job completion) must come after all required work. Detached or unreturned promises let the caller see success while work is pending or fails.

## Why AI
Which work is "required" before the boundary is a business decision, and fire-and-forget may be intended (and not always durable).

## AI prompt
"Identify the success boundary in this path. List work started before it that is not awaited or returned and that the contract needs completed. Give the failure-before/after and duplicate-invocation scenario that leaves wrong state. Report only with that scenario." Evidence: handler/consumer, called functions, ticket text on guarantees.

## Do not flag
- Work documented as best-effort (metrics, logging).
- Work handed to a durable queue whose enqueue is awaited.

## Examples
Flag:
```ts
app.post('/pay', (req, res) => { charge(req.body); res.sendStatus(200); });
```
OK:
```ts
app.post('/pay', async (req, res) => { await charge(req.body); res.sendStatus(200); });
```

## Evaluation cases
1. Charge not awaited before 200: flag.
2. Audit log not awaited before 200: ok.
3. Message acked before DB write finishes: flag.
4. Enqueue awaited then 202: ok.
