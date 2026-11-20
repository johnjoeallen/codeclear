# typescript/await-interleaving-race: Check-then-act or lazy init spanning an await

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Concurrency and request context; node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
A single event loop does not make code atomic across `await`. Two requests can interleave in a check-then-act, lazy initialization, memoized client or read-modify-write on shared in-process state.

## Why AI
Needs the shared-state owner, the invariant, and whether other safeguards (mutex, constraint) exist.

## AI prompt
"Construct an interleaving of two concurrent invocations around the `await` that violates a named invariant on shared state (map, cache, counter, singleton). Report only with that interleaving and its wrong outcome." Evidence: the function, definition of the shared state, callers' concurrency, any lock.

## Do not flag
- State local to one invocation.
- Sections with no `await` between the check and the act.
- Operations guarded by a mutex or by storing the in-flight promise.

## Examples
Flag:
```ts
if (!cache.has(k)) { cache.set(k, await load(k)); } // double load
```
OK:
```ts
if (!cache.has(k)) cache.set(k, load(k)); return cache.get(k);
```

## Evaluation cases
1. Balance read, `await`, then written back on module-level object: flag.
2. Promise stored in the map before awaiting: ok.
3. Check and set with no await between: ok.
