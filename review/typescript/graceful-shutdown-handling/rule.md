# typescript/graceful-shutdown-handling: SIGTERM path does not drain work or close clients

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Exceptions, resources, and shutdown; node-bug-hunter: Kafka, RabbitMQ, and job queues |

## Intent
On SIGTERM a service must stop accepting new work, finish or hand back in-flight requests, jobs, commits and acks, flush producers, and close clients within the platform's termination budget.

## Why AI
Needs the termination grace period, the set of long-lived resources and what in-flight work exists.

## AI prompt
"Trace the SIGTERM/shutdown path. Does it stop intake first, wait for in-flight requests/jobs/commits/acks, flush producers, and close DB/broker clients, all within the visible termination budget? Name the in-flight work lost or the resource left open. Report only with that scenario." Evidence: shutdown handler, server/consumer setup, `terminationGracePeriodSeconds` or equivalent.

## Do not flag
- Short-lived scripts, serverless handlers, frameworks with shutdown hooks enabled and shown (`app.enableShutdownHooks()`).

## Examples
Flag:
```ts
process.on('SIGTERM', () => process.exit(0)); // kills in-flight requests and unflushed producer
```
OK:
```ts
process.on('SIGTERM', async () => { server.close(); await consumer.stop(); await producer.disconnect(); await db.end(); });
```

## Evaluation cases
1. `process.exit` in SIGTERM handler with a Kafka consumer: flag.
2. Handler stops intake, awaits in-flight, closes clients: ok.
3. No handler but framework shutdown hooks enabled: ok.
