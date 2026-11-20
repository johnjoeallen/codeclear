# java/external-client-failure-handling: External client misreports failures

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts |

## Intent
A client wrapper that swallows exceptions, treats non-2xx responses as success, or returns defaults on failure makes callers act on data that was never obtained or written.

## Why AI
Correctness depends on what the caller does with the result and on the remote API's status semantics, which are outside the method body. Retry and timeout aspects are covered by `retry-without-bound-or-idempotency` and `missing-network-timeout`.

## AI prompt
"For each failure mode of this external call (exception, non-success status, empty body), what does the client return and what does the caller do with it? Report only where a failure is observably treated as success, with the concrete status/exception." Evidence: the client method, its callers, the remote API contract or error codes.

## Do not flag
- Clients that return a typed failure/result object the caller checks.
- Intentional best-effort calls (metrics, audit) documented as such.

## Examples
Flag: `try { http.post(url, body); } catch (Exception e) { return true; }`; or ignoring `response.getStatusCode()` on a payment call.
OK: `response.getStatusCode().is2xxSuccessful()` checked, otherwise throw `PaymentFailedException`.

## Evaluation cases
1. `catch (RestClientException e) { return Optional.empty(); }` where caller treats empty as "no account" - report.
2. `RestTemplate` default error handler throws on 4xx, propagated - no report.
3. Status 202 treated as completed for sync result - report.
4. Best-effort analytics ping swallowing errors - no report.
