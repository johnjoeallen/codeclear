# Tests: typescript/unbounded-concurrency

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: Promise.all over a query result (Examples: Flag)

covers: example:flag,required:promise-all

```typescript path=src/jobs.ts
export async function run(rows: Row[]) {
  await Promise.all(rows.map(async (r) => send(r)));  // CANDIDATE
}
```

## Case: a limiter, and a constant array (Examples: OK)

covers: example:ok,do-not-flag:limiter-and-constants,required:limiter

```typescript path=src/limited.ts
import pLimit from 'p-limit';
const limit = pLimit(5);
export async function run(rows: Row[]) {
  await Promise.all(rows.map(async (r) => limit(() => send(r))));
}
```

```typescript path=src/constants.ts
export async function run() {
  await Promise.all([1, 2, 3].map(async (n) => send(n)));
}
```

## Case: a worker or process per request

covers: detect:per-request-worker

```typescript path=src/server.ts
export function handler(req, res) {
  const w = new Worker('./job.js');  // CANDIDATE
  const p = spawn('convert', [req.query.file]);  // CANDIDATE
}
```

## Case: a data listener with no cap

covers: detect:accumulation

```typescript path=src/out.ts
export function a(proc) {
  const chunks = [];
  proc.stdout.on('data', (c) => chunks.push(c));  // CANDIDATE
}
export function b(proc) {
  let size = 0;
  proc.stdout.on('data', (c) => { size += c.length; if (size > 1e6) proc.kill(); });
}
```

## Case: a listener added in a handler or a loop

covers: detect:listeners

```typescript path=src/server.ts
export function handler(req, res) {
  bus.on('done', () => res.end());  // CANDIDATE
}
export function ok(req, res) {
  bus.once('done', () => res.end());
}
```

## Case: bounded by visible validation (Do not flag)

covers: do-not-flag:bounded-input

```typescript path=src/batch.ts
const MAX_BATCH = 50;
export async function run(items: Item[]) {
  if (items.length > MAX_BATCH) throw new Error('too many');
  await Promise.all(items.map(async (i) => send(i)));
}
```
