# java/rabbit-publisher-confirms-and-routing: Publishes without confirms or unroutable handling

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: RabbitMQ |

## Intent
Without publisher confirms and `mandatory`/returns handling, a message sent to a missing exchange or unroutable key is silently dropped even though the publish call returned.

## Why AI
It applies only when the flow requires delivery guarantees, which is a business decision visible only through the contract and configuration.

## AI prompt
"Does this flow require guaranteed delivery? If so, are publisher confirms (`publisher-confirm-type`) and returns (`mandatory`, `ReturnsCallback`) configured and handled? Report only if the guarantee is required and the visible config lacks them, naming the lost-message scenario." Evidence: publishing code, `RabbitTemplate`/connection factory config, the stated guarantee.

## Do not flag
- Best-effort notifications.
- Flows with confirms and returns handled.
- Config not supplied.

## Examples
Flag: `rabbitTemplate.convertAndSend("payments", key, msg)` for a payment event; no confirm type set, nothing handles returns.
OK: correlated confirms with a callback that retries or alerts, `setMandatory(true)` with a returns callback.

## Evaluation cases
1. Critical event, no confirms/returns - report.
2. Confirms + returns handled - no report.
3. Cache-invalidation hint, loss acceptable - no report.
4. Config not shown - no report.
