# typescript/overlapping-async-interval: setInterval with an async callback can overlap

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
`setInterval` fires on schedule regardless of whether the previous async run finished, so slow runs overlap, double-process work and pile up.

## Detect
`setInterval(cb, n)` where `cb` is an `async` function or its body calls an awaited operation (I/O, DB, HTTP) and there is no in-flight guard (a boolean/lock checked at the start) and the callback is not a recursive `setTimeout` pattern.

## Do not flag
- Callbacks with an `isRunning`/`busy` guard reset in `finally`.
- Callbacks that only do synchronous work.
- Recursive `setTimeout` scheduling after completion.

## Examples
Flag:
```ts
setInterval(async () => { await syncAll(); }, 1000);
```
OK:
```ts
const loop = async () => { try { await syncAll(); } finally { setTimeout(loop, 1000); } }; loop();
```

## Implementation notes
Anchor to the `setInterval` call. Multi-replica duplication: see `scheduled-task-overlap-replicas`.

Detected on `setInterval` with an inline `async` function or one that contains an `await`. A callback with an `if` that tests a name containing `running`, `busy`, `inflight`, `locked`, `pending` or `active` is treated as guarded. A callback passed by name is not followed.

## Required tests
Async interval without guard (finding), guard flag (no finding), sync callback (no finding), recursive setTimeout (no finding).

## Why deterministic
Async callback plus the absence of a guard variable are local syntactic facts.
