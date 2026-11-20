# Tests: typescript/path-traversal

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a file name from the request (Examples: Flag)

covers: example:flag,required:request-path

```typescript path=src/files.ts
export function download(req, res) {
  const name = req.query.file;
  res.sendFile(path.join('/data', name));  // CANDIDATE
}
```

## Case: basename and a base check (Examples: OK)

covers: example:ok,do-not-flag:checked,required:basename

```typescript path=src/files.ts
export function a(req, res) {
  const name = path.basename(req.query.file);
  res.sendFile(path.join('/data', name));
}
export function b(req, res) {
  const full = path.resolve('/data', req.query.file);
  if (!full.startsWith('/data/')) return res.sendStatus(400);
  res.sendFile(full);
}
```

## Case: fs calls and parameters named like paths

covers: detect:fs-calls

```typescript path=src/files.ts
export function read(filename: string) {
  return fs.readFileSync(filename);  // CANDIDATE
}
export function drop(req) {
  fs.unlink(req.body.path, () => {});  // CANDIDATE
}
export function stream(req, res) {
  fs.createReadStream(req.params.name).pipe(res);  // CANDIDATE
}
```

## Case: server-generated names and constants (Do not flag)

covers: do-not-flag:server-names

```typescript path=src/files.ts
export function a(id: string) {
  return fs.readFileSync(`/data/${id}.json`);
}
export function b() {
  return fs.readFileSync('/etc/app.conf');
}
export function c(res) {
  res.sendFile('index.html', { root: '/public' });
}
```

## Case: a test file

covers: detect:test-file

```typescript path=src/files.test.ts
export function read(filename: string) {
  return fs.readFileSync(filename);
}
```
