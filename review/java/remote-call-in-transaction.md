# java/remote-call-in-transaction: Remote call or sleep inside an open database transaction

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Spring; java-bug-hunter: Transactions and persistence boundaries |

## Intent
HTTP calls, message sends or sleeps while a transaction is open hold locks and a pooled connection for the duration, and a failed call after a write leaves inconsistent side effects.

## Detect
In a method annotated `@Transactional` (or inside `TransactionTemplate.execute`), a call on a `RestTemplate`, `WebClient`, `HttpClient`, Feign client, `KafkaTemplate`, `RabbitTemplate`, SQS/SNS client, or `Thread.sleep`.

## Do not flag
- `@TransactionalEventListener(phase = AFTER_COMMIT)` handlers.
- Methods with `propagation = NOT_SUPPORTED` or `NEVER`.
- Calls in a different, non-transactional method.

## Examples
Flag:
```java
@Transactional void pay(Order o) { repo.save(o); gateway.charge(o); }
```
OK: charge after the transaction commits, or via an outbox.

## Implementation notes
AST plus a field-type match for client classes. Anchor to the remote call. Dual-write failure windows are in `dual-write-without-outbox`.

## Required tests
`@Transactional` + RestTemplate (candidate), AFTER_COMMIT listener (none), non-transactional method (none).

## Why hybrid
Whether a concrete lock hold or inconsistency results depends on call latency and what the transaction wrote.

## AI prompt
"Describe the concrete lock/connection hold or inconsistent-side-effect scenario caused by this remote call inside the transaction; report only if one exists." Evidence: the method, writes before the call, the client's behaviour and transaction configuration.
