# Tests: typescript/stream-backpressure-and-errors

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: pipe with no error handler (Examples: Flag)

covers: example:flag,required:pipe

```typescript path=src/copy.ts
export function copy(src, dst) {
  fs.createReadStream(src).pipe(fs.createWriteStream(dst));  // CANDIDATE
}
```

## Case: pipeline (Examples: OK)

covers: example:ok,do-not-flag:pipeline,required:pipeline

```typescript path=src/copy.ts
import { pipeline } from 'stream';
export function copy(src, dst, cb) {
  pipeline(fs.createReadStream(src), fs.createWriteStream(dst), cb);
}
```

## Case: write in a loop

covers: detect:write-loop

```typescript path=src/dump.ts
export function dump(out, rows) {
  for (const r of rows) {
    out.write(JSON.stringify(r));  // CANDIDATE
  }
}
export function dumpDrain(out, rows) {
  for (const r of rows) {
    if (!out.write(JSON.stringify(r))) { out.once('drain', () => {}); }
  }
}
```

## Case: a data listener that writes

covers: detect:data-listener

```typescript path=src/relay.ts
export function relay(src, dst) {
  src.on('data', (chunk) => dst.write(chunk));  // CANDIDATE
}
```

## Case: small in-memory streams and error listeners (Do not flag)

covers: do-not-flag:small-and-handled

```typescript path=src/small.ts
export function a(src, dst) {
  src.on('error', (e) => log(e));
  dst.on('error', (e) => log(e));
  src.pipe(dst);
}
```
