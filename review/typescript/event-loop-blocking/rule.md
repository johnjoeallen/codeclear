# typescript/event-loop-blocking: Synchronous or CPU-heavy work on the Node event loop

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety; node-bug-hunter: Async completion and error ownership |

## Intent
Blocking calls stall every request on the process. Includes sync fs/crypto/child_process, large-loop CPU work, catastrophic regex backtracking and recursive microtask scheduling that starves I/O.

## Detect
- `fs.*Sync`, `crypto.pbkdf2Sync/scryptSync/randomBytesSync`, `execSync`, `spawnSync`, `zlib.*Sync`, `JSON.parse/stringify` on request-sized data inside request handlers or consumers (not at startup).
- Regex literals with nested quantifiers (`(a+)+`, `(.*)*`) applied to user input.
- A function that re-queues itself with `queueMicrotask` / `Promise.resolve().then` recursion without yielding.

## Do not flag
- Sync calls at module load, CLI scripts, build tools, or in tests.
- Browser code.

## Examples
Flag:
```ts
app.get('/f', (req, res) => res.send(fs.readFileSync(req.query.p as string)));
```
OK:
```ts
app.get('/f', async (req, res) => res.send(await fs.promises.readFile(p)));
```

## Implementation notes
Equivalent: `n/no-sync`. Anchor to the call; decide "inside a handler" by enclosing function context.

## Required tests
`readFileSync` in handler (finding), at module top (no finding), nested-quantifier regex on input (finding), `readFile` async (no finding).

## AI prompt
"With realistic input sizes, does this block the event loop long enough to stall other requests?" Evidence: the call, input source and size, whether it runs per request.

## Why hybrid
Blocking APIs are listable; the impact depends on input size and frequency.
