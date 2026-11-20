# typescript/timer-listener-leak: Timers or listeners not released on every exit path (Node)

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety; Exceptions, resources, and shutdown; node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Timers and listeners created per call and cleared only on success leak on failure, cancellation and shutdown, keep the process alive and accumulate duplicates. (React effects: `effect-without-cleanup`.)

## Detect
- `setTimeout/setInterval` whose id is not stored, or stored but cleared only in the success branch (no `finally`).
- `emitter.on/once/addEventListener` inside a function called repeatedly with no matching `off/removeListener`, or a promise that awaits an `once` event without removing the other listener on failure.
- Timers not `unref()`ed or cleared in a shutdown handler for long-lived services.

## Do not flag
- Module-level singletons registered once.
- `once` listeners that are removed by firing.

## Examples
Flag:
```ts
const t = setTimeout(onTimeout, 5000); await work(); clearTimeout(t); // leaks when work throws
```
OK:
```ts
const t = setTimeout(onTimeout, 5000); try { await work(); } finally { clearTimeout(t); }
```

## Implementation notes
Anchor to the creation call.

Detected on an expression statement `setInterval(...)` inside a function (the id is thrown away); on a stored timer in a function that has a `clearTimeout`/`clearInterval` but not in a `finally`, when the function awaits; and on `on`/`addListener`/`addEventListener` inside a function that mentions no `off`, `removeListener`, `removeEventListener`, `once` or `signal`, is not named like an initializer, and is not a `useEffect` callback. Module-level listeners are never matched. Timers that are not `unref()`ed are not detected.

## Required tests
Clear after await without `finally` (finding), `finally` (no finding), listener in handler without off (finding), module-level listener (no finding).

## AI prompt
"On failure, cancellation or shutdown does the timer or listener stay registered, and does that cause duplicated work or a hung process?" Evidence: function, callers, shutdown code.

## Why hybrid
Lifetimes depend on how often and by whom the function is invoked.
