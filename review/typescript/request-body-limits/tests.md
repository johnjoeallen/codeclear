# Tests: typescript/request-body-limits

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: multer with no limits (Examples: Flag)

covers: example:flag,required:multer

```typescript path=src/upload.ts
export const upload = multer({ dest: 'uploads/' });  // CANDIDATE
```

## Case: multer with limits (Examples: OK)

covers: example:ok,do-not-flag:limits-set,required:limits

```typescript path=src/upload.ts
export const upload = multer({ dest: 'uploads/', limits: { fileSize: 5_000_000, files: 3 } });
app.use(express.json({ limit: '100kb' }));
```

## Case: busboy, formidable and a huge limit

covers: detect:parsers

```typescript path=src/upload.ts
const b = busboy({ headers: req.headers });  // CANDIDATE
const f = formidable({ uploadDir: '/tmp' });  // CANDIDATE
const g = formidable({ maxFileSize: 5 * 1024 * 1024 });
app.use(bodyParser.json({ limit: '500mb' }));  // CANDIDATE
app.use(express.json({ limit: Infinity }));  // CANDIDATE
```

## Case: a body read by hand

covers: detect:manual

```typescript path=src/raw.ts
export function a(req) {
  let body = '';
  req.on('data', (c) => { body += c; });  // CANDIDATE
}
export function b(req) {
  let body = '';
  let size = 0;
  req.on('data', (c) => {
    size += c.length;
    if (size > 1e6) req.destroy();
    body += c;
  });
}
```

## Case: a limit set by a reverse proxy is left to the AI (Do not flag)

covers: do-not-flag:proxy-limit

```typescript path=src/upload.ts
// nginx client_max_body_size 10m is configured in front of this service
export const upload = multer({ dest: 'uploads/' });  // CANDIDATE
```
