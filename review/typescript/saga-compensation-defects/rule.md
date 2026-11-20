# typescript/saga-compensation-defects: Saga compensation order, failure and repeated execution

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects |

## Intent
When compensation steps are supplied they must run in a safe order, handle their own failure and be safe to run more than once.

## Why AI
Compensation semantics are business-defined and span services.

## AI prompt
"For the supplied saga, is compensation order correct (reverse of completion)? What if a compensation fails or runs twice? Name the resulting inconsistent state. Do not require a saga for every multi-service operation." Evidence: saga/step definitions, compensating actions, retry behavior.

## Do not flag
- Operations with a single remote write.
- Compensations that are idempotent and retried.

## Examples
Flag:
```ts
try { await reserve(); await charge(); } catch { await refund(); await release(); } // refund runs even if charge never happened
```
OK: track completed steps and compensate only those, idempotently.

## Evaluation cases
1. Refund compensates an uncharged payment: flag.
2. Compensation throws and swallows, leaving reservation held: flag.
3. Idempotent compensation of only completed steps: ok.
