# typescript/eventemitter-unhandled-error: EventEmitter error events and async listener rejections

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Promises, cancellation, and event-loop safety |

## Intent
An emitter (stream, socket, client, custom) that emits `'error'` with no listener throws and can crash the process. A rejected `async` listener is not caught unless `captureRejections` is set.

## Detect
- A variable created from `new EventEmitter()`, a stream/socket/`net`/`http` request/client, or `fs.createReadStream` that has `.on('data'|'message'|...)` but no `.on('error')`, `once('error')` or `pipeline` in the file.
- `async` listener registered with `.on` where the emitter is not created with `{ captureRejections: true }` and the body has no try/catch.

## Do not flag
- Emitters managed by `stream.pipeline`, `finished` or `events.once` that handle errors.
- Libraries known to register their own error handler (shown in config).

## Examples
Flag:
```ts
const s = fs.createReadStream(p); s.on('data', handle);
```
OK:
```ts
const s = fs.createReadStream(p); s.on('error', fail); s.on('data', handle);
```

## Implementation notes
Anchor to the emitter creation.

Detected on `x.on("data"|"message"|"close"|"end"|"connect"|"open"|"readable"|"finish"|"drain"|"response"|"exit", ...)` where `x` is declared in the file from `new EventEmitter()`, a socket or stream class, `net`/`tls`/`http(s)` connect/request/createServer, `fs.createReadStream/WriteStream` or `spawn`, and the file has no `x.on("error")`, `once("error")` or `addListener("error")` and mentions no `pipeline(`, `finished(` or `events.once`. The `captureRejections` check for async listeners is covered by `async-callback-ignored-by-api`.

## Required tests
Stream with data only (finding), error listener present (no finding), `pipeline` (no finding), async listener without catch (finding).

## AI prompt
"Can this emitter emit 'error' (or can the async listener reject) with nothing handling it? Check emitter type and configuration." Evidence: emitter creation, listeners, library docs/version.

## Why hybrid
Which objects emit `'error'` and their configuration require type and library knowledge.
