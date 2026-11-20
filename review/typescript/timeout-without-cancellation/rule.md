# typescript/timeout-without-cancellation: Promise.race timeout that does not cancel the work

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
A `Promise.race([work, timeout])` only stops waiting. The request, query or side effect continues, keeps its pool slot, and may commit after the caller starts a retry.

## Detect
`Promise.race` with an operand that is a timer-based rejecting promise (`setTimeout` + `reject`, `delay`) and no `AbortSignal` / `AbortController` / `signal:` option passed into the raced operation; also functions taking a `deadline`/`timeout` parameter that never forward it to the client call.

## Do not flag
- Operations given `AbortSignal.timeout(...)` or `signal`.
- Pure in-memory computations with no side effects.

## Examples
Flag:
```ts
await Promise.race([fetch(url), sleepReject(5000)]);
```
OK:
```ts
await fetch(url, { signal: AbortSignal.timeout(5000) });
```

## Implementation notes
Anchor to `Promise.race`.

Two files. `rule.dsl` flags `Promise.race([...])` where one element mentions `setTimeout`, `delay(`, `sleep(`, `timeout(` or `wait(` and another does not, with no `signal`, `AbortSignal` or `AbortController` in the array. `params.dsl` flags a function with a parameter named `timeout`, `timeoutMs` or `deadline` that is never used again in the function.

## Required tests
Race without signal (finding), `AbortSignal.timeout` (no finding), signal forwarded to driver (no finding).

## AI prompt
"After the timeout fires, what is still running, who observes its completion, and can it commit or double-apply while the caller retries?" Evidence: the race, the client API's cancellation support, retry logic.

## Why hybrid
The pattern is syntactic; harm depends on side effects and the client's cancellation support.
