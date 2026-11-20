# Tests: typescript/timeout-without-cancellation

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a race against a timer (Examples: Flag)

covers: example:flag,required:race

```typescript path=src/call.ts
export async function call() {
  return Promise.race([  // CANDIDATE
    fetchAll(),
    new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 3000)),
  ]);
}
```

## Case: an AbortSignal (Examples: OK)

covers: example:ok,do-not-flag:abort-signal,required:signal

```typescript path=src/call.ts
export async function call() {
  return fetch(url, { signal: AbortSignal.timeout(3000) });
}
export async function raced(signal: AbortSignal) {
  return Promise.race([fetchAll({ signal }), delay(3000)]);
}
```

## Case: a timeout parameter that is never used

covers: detect:unused-parameter

```typescript path=src/client.ts
export function get(url: string, timeout: number) {  // CANDIDATE
  return fetch(url);
}
export function post(url: string, timeoutMs: number) {
  return fetch(url, { signal: AbortSignal.timeout(timeoutMs) });
}
```

## Case: pure in-memory races (Do not flag)

covers: do-not-flag:in-memory

```typescript path=src/mem.ts
export async function first(a: Promise<number>, b: Promise<number>) {
  return Promise.race([a, b]);
}
```
