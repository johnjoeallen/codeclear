# typescript/async-callback-ignored-by-api: async callback passed to an API that does not await it

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
Callbacks given to `EventEmitter.on`, `setTimeout`, `setInterval`, Express middleware (pre-5), `Array.reduce/filter/some`, `process.on` or similar APIs have their returned promise ignored, so failures are lost and work can finish after the caller reports success. `forEach` is covered by `async-callback-in-foreach`.

## Detect
An `async` function or arrow passed as a callback to a known non-awaiting API: `.on/.once/.addListener`, `setTimeout`, `setInterval`, `setImmediate`, `process.nextTick`, `.filter/.some/.every/.reduce/.sort`, or a transaction/ORM callback whose promise-creating inner calls are not `await`ed or returned.

## Do not flag
- `map` feeding `Promise.all`/`allSettled`.
- Callbacks whose whole body is wrapped in `try/catch` that reports the error.
- Express 5 or wrapped handlers that forward rejections.
- Test framework hooks that await promises.

## Examples
Flag:
```ts
emitter.on('order', async (o) => { await save(o); });
```
OK:
```ts
emitter.on('order', (o) => { save(o).catch(logError); });
```

## Implementation notes
Equivalent: `@typescript-eslint/no-misused-promises`. Anchor to the callback.

Detected on an `async` arrow or function expression passed to `on`/`once`/`addListener` (when it awaits), to `setTimeout`/`setInterval`/`setImmediate`/`process.nextTick`/`queueMicrotask`, and to `filter`/`some`/`every`/`find`/`findIndex`/`sort`/`reduce`. A callback whose body is a single `try` statement is treated as reporting its own errors. `map` is never matched, and transaction and ORM callbacks are not recognised.

## Required tests
Async `on` listener without catch (finding), listener with internal try/catch (no finding), `filter(async ...)` (finding), `map` + `Promise.all` (no finding).

## AI prompt
"Does the receiving API await or handle the returned promise? Name the lost failure or early completion." Evidence: callback, the API's docs/version, wrapper code, library config.

## Why hybrid
Syntax finds async callbacks; whether the receiver handles the promise depends on the library and version.
