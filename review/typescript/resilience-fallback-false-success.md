# typescript/resilience-fallback-false-success: Fallbacks, breakers and propagation hide failure

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects; node-bug-hunter: Distributed workflows and microservices |

## Intent
A circuit-breaker fallback, default value or bulkhead can report false success, bypass authorization, or return materially stale data. Deadline budgets and trace/auth headers must propagate to downstream calls.

## Why AI
Whether a fallback value is acceptable is a business decision.

## AI prompt
"What does the fallback return when the dependency fails, and could a caller treat it as real data (e.g. `allowed: true`, empty list as 'no results', zero balance)? Is auth/trace/deadline context dropped on the downstream call in a way that causes exposure or wrong behavior? Report only with a demonstrated failure, exposure or false success; do not require breakers or bulkheads." Evidence: fallback code, consumers of the value, downstream contract.

## Do not flag
- Fallbacks that surface degraded status, or stale data acceptable per requirements.

## Examples
Flag:
```ts
const allowed = await breaker.fire().catch(() => true); // permission service down => allow
```
OK: fail closed or return an explicit degraded error.

## Evaluation cases
1. Fallback returns `[]` for a fraud check: flag.
2. Fallback returns cached product list labelled stale: ok.
3. Downstream call drops the user token and runs with a service identity returning other users' data: flag.
