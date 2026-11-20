# java/rabbit-connection-per-message: Connection or channel created per message

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | PERFORMANCE |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: RabbitMQ |

## Intent
Opening a RabbitMQ connection or channel for each message is slow and leaks connections or channels if not closed.

## Detect
A call to `connectionFactory.newConnection()`, `createConnection()`, `connection.createChannel()` inside a method that publishes/consumes a single message, or in a loop, where the result is not closed in try-with-resources/`finally`, or is created on every call rather than injected/reused.

## Do not flag
- Spring-managed `RabbitTemplate`/`CachingConnectionFactory` usage.
- A channel created in `execute(ChannelCallback)` or in a lifecycle/init method.
- Channels closed in try-with-resources used once per bounded batch.

## Examples
Flag:
```java
void publish(Msg m) { Connection c = factory.newConnection(); Channel ch = c.createChannel(); ch.basicPublish(...); }
```
OK: inject `RabbitTemplate` and call `convertAndSend`.

## Implementation notes
AST. Anchor to the creation call.

## Required tests
`newConnection` in publish method (candidate), `RabbitTemplate` (none), try-with-resources (candidate only if per message), init-time creation (none).

## Why hybrid
Pattern is syntactic; whether a managed factory or container lifecycle already handles it needs context.

## AI prompt
"Is a new connection/channel created and discarded per message, and does it leak or exhaust connections? Account for Spring-managed factories and lifecycle." Evidence: method, factory bean definitions, caller frequency.
