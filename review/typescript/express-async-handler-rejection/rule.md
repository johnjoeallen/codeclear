# typescript/express-async-handler-rejection: Rejected async route handler not forwarded to error handling

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
Express 4 ignores the promise of an `async` handler. A thrown error becomes an unhandled rejection and the request hangs. Express 5 forwards rejections.

## Detect
An `async` function registered with `app|router.get/post/put/patch/delete/use` that awaits operations and has no enclosing `try/catch` calling `next(err)`, and is not wrapped in `asyncHandler`/`express-async-errors`/similar. Check `package.json` express version when available.

## Do not flag
- Express >= 5, Fastify, NestJS, Koa, which handle rejections.
- Handlers wrapped by an async wrapper or with whole-body try/catch that responds or calls `next`.

## Examples
Flag (Express 4):
```ts
app.get('/u', async (req, res) => { res.json(await getUser()); });
```
OK:
```ts
app.get('/u', asyncHandler(async (req, res) => { res.json(await getUser()); }));
```

## Implementation notes
Anchor to the route registration.

Detected on `app`/`router`/`*Router`/`server` route and `use` registrations with an `async` function argument that contains an `await` and no `try`. A file that mentions `fastify`, `@nestjs`, `koa`, `hapi`, `express-async-errors`, `asyncHandler`, `express-async-handler`, `catchAsync` or `wrapAsync`, or that declares Express 5, is skipped; a handler wrapped in a call is not an `async` function, so it is not matched. The version in `package.json` is not read.

## Required tests
Bare async handler on Express 4 (finding), wrapped (no finding), try/catch with `next(e)` (no finding), Express 5 (no finding).

## AI prompt
"Which Express version and wrappers apply? Does a rejection here reach the error middleware?" Evidence: handler, `package.json`, app setup.

## Why hybrid
The handler shape is syntactic; the framework version and global patches decide behavior.
