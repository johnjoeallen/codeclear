# typescript/request-body-limits: Missing body, upload or parser limits

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
Unlimited body parsing or uploads let one request exhaust memory or disk; wrong parser/content-type handling returns empty or misparsed bodies.

## Detect
- `multer()` / `busboy` / `formidable` with no `limits`/`maxFileSize`.
- `bodyParser.*({ limit: '100mb' })` or very large limits, `express.json({ limit: Infinity })`.
- Manual `req.on('data', c => body += c)` with no size check.
- `Buffer.concat` of request chunks without a cap.

## Do not flag
- Reverse-proxy limits shown in config.
- Admin-only internal routes with documented large limits.

## Examples
Flag:
```ts
const upload = multer({ dest: 'u/' });
```
OK:
```ts
const upload = multer({ dest: 'u/', limits: { fileSize: 5_000_000, files: 1 } });
```

## Implementation notes
Anchor to the parser construction.

## Required tests
`multer` without limits (finding), with limits (no finding), manual accumulation without cap (finding), default `express.json()` (no finding).

## AI prompt
"Is the route reachable by untrusted callers and is no limit applied here or by a visible gateway? What size triggers failure?" Evidence: route, parser config, gateway config.

## Why hybrid
Missing options are syntactic; exposure and gateway limits are contextual.
