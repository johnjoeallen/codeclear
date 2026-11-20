# java/rabbit-durability-and-ttl-contract: Queue/message durability, TTL and DLX mismatch

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: RabbitMQ |

## Intent
Non-durable queues/exchanges, transient messages, short TTLs or missing dead-letter routing contradict a message's required durability or retry contract, losing messages on broker restart or expiry.

## Why AI
Whether the settings are wrong depends on the durability/retry contract of the message, evident only from documentation, naming or surrounding code.

## AI prompt
"State the durability and retry contract this message needs from the change and context. Compare it with the declared queue/exchange durability, delivery mode, TTL and dead-letter arguments. Report only a concrete mismatch (e.g. durable contract but `durable=false`), naming the loss scenario." Evidence: declarations (`Queue`, `Exchange`, `@RabbitListener` bindings), message properties, ticket or comments.

## Do not flag
- Transient data where loss is accepted (RPC replies, ephemeral events).
- Contracts that are not clear from the change.

## Examples
Flag: durable order events declared with `new Queue("orders", false)` and `MessageDeliveryMode.NON_PERSISTENT`.
OK: `QueueBuilder.durable("orders").deadLetterExchange("orders.dlx").build()` with persistent messages.

## Evaluation cases
1. Order queue non-durable, persistent contract claimed - report.
2. Reply queue exclusive and auto-delete - no report.
3. TTL of 1s on a retry queue meant to wait 1 min - report.
4. Contract unclear - no report.
