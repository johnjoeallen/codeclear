# typescript/lambda-lifecycle-hazards: Unfinished work, warm-instance state and connection pressure in Lambda

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless; node-bug-hunter: AWS and serverless failure windows |

## Intent
After the handler returns, the runtime may freeze, so detached promises can be lost. Module state persists only within a warm instance, and each instance opens its own connections, which can exhaust a database. Retries depend on the invocation source.

## Why AI
Needs the invocation type (sync, async, event source), concurrency and pool sizes; warm state is not shared across all instances.

## AI prompt
"Does work start but not complete before the handler returns? Is request-specific data kept in module state that a warm invocation reuses? Can max concurrency times pool size exceed the database's capacity? What does the actual invoker do on error (retry, DLQ)? Do not assume detached promises complete or that warm state is shared by all instances." Evidence: handler, module scope, pool config, concurrency settings, event source.

## Do not flag
- Awaited handlers, intentional connection reuse, idempotent retried work.

## Examples
Flag:
```ts
export const handler = async (e) => { sendAudit(e); return { statusCode: 200 }; }; // not awaited
```
OK: `await sendAudit(e)` before returning.

## Evaluation cases
1. Fire-and-forget SDK call before return: flag.
2. Module-level `let currentUser` set per invocation: flag.
3. Module-level DB client reused across invocations: ok.
