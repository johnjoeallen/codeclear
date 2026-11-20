# typescript/unbounded-concurrency: Unbounded Promise.all, queues, workers or recursion

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety; node-review: Exceptions, resources, and shutdown |

## Intent
Starting one task per element of a large input exhausts sockets, DB pool, memory or downstream capacity. Also applies to unbounded buffering, uploads, subprocess output, listener growth and worker creation.

## Detect
- `Promise.all(xs.map(async ...))` where `xs` comes from a query, request body, file list or paginated result, with no `p-limit`, `p-map` concurrency, chunking or semaphore.
- `new Worker(...)` or `spawn/fork` per request/message.
- Accumulation (`chunks.push`, `+=` on a Buffer/string) from a stream or subprocess with no size cap.
- `emitter.on(...)` inside a handler or loop.

## Do not flag
- Arrays that are small constants or bounded by visible validation.
- Visible pools or limiters.

## Examples
Flag:
```ts
await Promise.all(userIds.map((id) => fetchUser(id)));
```
OK:
```ts
const limit = pLimit(10); await Promise.all(userIds.map((id) => limit(() => fetchUser(id))));
```

## Implementation notes
Anchor to the fan-out call.

## Required tests
Unbounded map over query result (finding), `pLimit` (no finding), literal 3-element array (no finding), listener added in loop (finding).

## AI prompt
"Is the collection size attacker- or data-controlled, and does the visible limit or downstream capacity prevent exhaustion? Do not assume an unseen pool is unbounded." Evidence: source of the array, limits, dependency capacity.

## Why hybrid
Fan-out is syntactic; whether size is bounded or the resource scarce is contextual.
