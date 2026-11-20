# java/inter-service-call-resilience: Missing resilience controls on inter-service calls

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Spring and distributed-service behavior |

## Intent
A changed call to another service may need a circuit breaker, bulkhead, trace propagation or idempotency key. These are required only when the changed behaviour and a failure scenario make them necessary.

## Why AI
They are not defects by themselves; necessity depends on criticality, traffic, retry behaviour and the platform's existing resilience (gateway, service mesh, shared client config). Timeouts are covered by `missing-network-timeout`.

## AI prompt
"For this new or changed inter-service call, describe a concrete failure scenario (dependency slow or down, duplicate retry, lost trace) and whether the shown code or client configuration already handles it. Report only when the scenario plainly applies and no mitigation is visible. Do not require patterns for their own sake." Evidence: the call, client/resilience configuration, retry behaviour, request volume, whether the operation is a write.

## Do not flag
- Calls already wrapped by shared resilience configuration.
- Non-critical calls with graceful degradation.
- Blanket "add a circuit breaker" requests.

## Examples
Flag: a synchronous call in a hot request path to an unreliable partner API, retried by a client library, with no breaker and a POST that creates a charge without an idempotency key.
OK: call wrapped via a configured `@CircuitBreaker` with fallback.

## Evaluation cases
1. Retried POST charge without idempotency key - report.
2. Read call with timeout and shared breaker config - no report.
3. Fire-and-forget metrics call - no report.
4. New hot-path call, no breaker, no config shown, dependency known flaky in the ticket - report.
