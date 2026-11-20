# typescript/stream-backpressure-and-errors: Stream piping without backpressure, error propagation or cleanup

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Exceptions, resources, and shutdown; node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
`a.pipe(b)` does not forward errors or destroy the other end, leaking file descriptors and hanging responses. Ignoring the `write()` return value skips backpressure and buffers unboundedly.

## Detect
- `.pipe(` chains with no `.on('error')` on every stream, or not replaced by `stream.pipeline` / `stream/promises` `pipeline`.
- A loop calling `writable.write(chunk)` without checking its return value or awaiting `drain`.
- `readable.on('data', ...)` pushing into a slower writable with no pause/drain handling.

## Do not flag
- `pipeline(...)`/`finished(...)` use, managed resources, small in-memory streams.
- Do not report a leak solely because no manual close appears.

## Examples
Flag:
```ts
fs.createReadStream(p).pipe(res);
```
OK:
```ts
await pipeline(fs.createReadStream(p), res);
```

## Implementation notes
Anchor to the `pipe` call or write loop.

Detected on `.pipe(` in a file with no `.on("error")` and no `pipeline(`, `finished(` or `stream/promises`; on a `write(...)` statement directly inside a loop in a function that never mentions `drain`; and on `.on("data", ...)` whose callback writes and never pauses. A leak is not reported just because no `close` appears.

## Required tests
`pipe` without error handling (finding), `pipeline` (no finding), write loop ignoring return value (finding), `drain` awaited (no finding).

## AI prompt
"If either end fails or the consumer is slow, does the other end leak or does memory grow unboundedly? Cite the failing end." Evidence: stream code, runtime context, sizes.

## Why hybrid
Pattern detection is syntactic; managed lifetimes and data volume decide impact.
