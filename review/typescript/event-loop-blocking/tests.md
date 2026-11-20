# Tests: typescript/event-loop-blocking

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a sync call in a request handler (Examples: Flag)

covers: example:flag,required:sync-in-handler

```typescript path=src/api.ts
export function handler(req, res) {
  const data = fs.readFileSync('big.json');  // CANDIDATE
  res.send(data);
}
```

## Case: async versions, and sync calls at start-up (Examples: OK)

covers: example:ok,do-not-flag:module-load,required:async

```typescript path=src/api.ts
const config = fs.readFileSync('config.json');
export async function handler(req, res) {
  const data = await fs.promises.readFile('big.json');
  res.send(data);
}
```

## Case: crypto, child processes and zlib

covers: detect:sync-calls

```typescript path=src/api.ts
export function a(req, res) {
  crypto.pbkdf2Sync(req.body.pw, salt, 100000, 64, 'sha512');  // CANDIDATE
  execSync('ls');  // CANDIDATE
  zlib.gzipSync(req.body);  // CANDIDATE
}
```

## Case: a consumer

covers: detect:consumer

```typescript path=src/consumer.ts
export function onMessage(message) {
  fs.writeFileSync('out.txt', message.value);  // CANDIDATE
}
```

## Case: scripts, build tools and plain functions (Do not flag)

covers: do-not-flag:scripts-and-tools

```typescript path=scripts/seed.ts
export function seed(req) {
  fs.readFileSync('seed.json');
}
```

```typescript path=src/util.ts
export function plain(path: string) {
  return fs.readFileSync(path);
}
```

## Case: a regular expression with nested quantifiers

covers: detect:redos

```typescript path=src/valid.ts
export function a(s: string) {
  return /(a+)+$/.test(s);  // CANDIDATE
}
export function b(s: string) {
  return /^[a-z]+$/.test(s);
}
```
