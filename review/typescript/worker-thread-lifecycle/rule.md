# typescript/worker-thread-lifecycle: Worker thread messages, failures and shared buffers

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
A caller waiting on a `Worker` can hang or lose work if the worker errors, exits, is terminated, or never replies; shared or transferred buffers can race or become detached.

## Why AI
Needs the protocol between parent and worker and which messages are guaranteed; cannot be seen from one file.

## AI prompt
"Walk the parent promise: what happens on worker `error`, non-zero `exit`, `terminate()` or a missing reply? Does the caller hang or drop work? For `SharedArrayBuffer`/transferred buffers, is there an unsynchronised access? Give the concrete trigger." Evidence: parent and worker files, message protocol, termination code.

## Do not flag
- Workers with `error`/`exit` handlers that reject the waiting promise.
- Request-reply pairs with timeouts.

## Examples
Flag:
```ts
return new Promise((res) => { w.on('message', res); w.postMessage(job); }); // never settles if worker crashes
```
OK:
```ts
return new Promise((res, rej) => { w.once('message', res); w.once('error', rej); w.once('exit', (c) => c && rej(new Error(`exit ${c}`))); w.postMessage(job); });
```

## Evaluation cases
1. Only a `message` listener: flag.
2. `error` and `exit` reject the promise: ok.
3. Two threads write one `SharedArrayBuffer` without Atomics: flag.
