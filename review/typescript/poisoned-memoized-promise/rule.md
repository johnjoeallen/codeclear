# typescript/poisoned-memoized-promise: Rejected initialization promise cached permanently

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Caching the promise of a client or connection init in a module variable makes one transient failure permanent, since every later call receives the same rejected promise.

## Detect
A module-level variable assigned the result of an async call (`client ??= connect()`, `if (!p) p = init()`) and returned or awaited by callers, with no `.catch(() => { p = undefined; throw ... })` or try/catch that resets it.

## Do not flag
- Initialization that resets the cache on failure.
- Promises for operations that must fail permanently by design.

## Examples
Flag:
```ts
let db: Promise<Db>; export const getDb = () => (db ??= connect());
```
OK:
```ts
export const getDb = () => (db ??= connect().catch((e) => { db = undefined; throw e; }));
```

## Implementation notes
Anchor to the assignment.

Detected on `x = f()` or `x ??= f()` where `x` is declared at the top of the module, the right side is a call that is not awaited and has no `.catch`, the assignment is not inside a `try`, and no assignment in the file sets `x` back to `undefined` or `null`. Whether `f()` really returns a promise is not known; any call is treated as one.

## Required tests
Cached without reset (finding), reset on catch (no finding), cached resolved value (no finding).

## AI prompt
"After one rejected initialization, do later calls keep receiving the failure with no recovery?" Evidence: the module, callers, retry/restart behavior.

## Why hybrid
The pattern is local; whether failure is recoverable by design is not.
