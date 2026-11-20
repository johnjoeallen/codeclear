# typescript/rabbitmq-publisher-confirms: Publisher confirms, unroutable messages and durability

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: RabbitMQ and background jobs |

## Intent
A plain channel `publish` returns before the broker accepts the message. Without a confirm channel, `mandatory`+`return` handling, persistent delivery and durable queues, a message the contract needs can be silently lost. Channels and connections must be closed.

## Detect
- `connection.createChannel()` used with `publish`/`sendToQueue` rather than `createConfirmChannel`, or confirm callback/`waitForConfirms` ignored.
- `publish` without `{ persistent: true }` to a durable queue where durable delivery is claimed.
- Channel/connection created per call with no `close()` on all paths.

## Do not flag
- Best-effort telemetry or transient messages.
- Shared long-lived channels.

## Examples
Flag:
```ts
const ch = await conn.createChannel(); ch.sendToQueue(q, buf);
```
OK:
```ts
const ch = await conn.createConfirmChannel(); await new Promise((r, j) => ch.sendToQueue(q, buf, { persistent: true }, (e) => (e ? j(e) : r(null))));
```

## Implementation notes
Anchor to the publish call.

Detected on `createChannel()` in a file that publishes and never mentions `createConfirmChannel`, `waitForConfirms` or `confirm:`; on `publish`/`sendToQueue` without `persistent: true` in a file that declares a `durable: true` queue; and on `connect`/`createChannel` inside a function that never calls `close` or `disconnect` and is not named like an init function. A file that mentions telemetry, metrics, analytics, best-effort or transient is skipped.

## Required tests
Plain channel publish (finding), confirm channel with callback (no finding), ignored callback (finding), persistent flag set (no finding).

## AI prompt
"Does this message need durable, confirmed delivery per the contract? What is lost if the broker does not accept it?" Evidence: publish, queue declaration, delivery requirements.

## Why hybrid
API usage is syntactic; whether the loss matters is a requirement.
