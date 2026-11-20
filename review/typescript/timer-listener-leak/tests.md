# Tests: typescript/timer-listener-leak

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an interval whose id is dropped (Examples: Flag)

covers: example:flag,required:dropped-id

```typescript path=src/poll.ts
export function start() {
  setInterval(poll, 1000);  // CANDIDATE
}
```

## Case: an interval that is kept and cleared (Examples: OK)

covers: example:ok,do-not-flag:module-level,required:kept

```typescript path=src/poll.ts
let timer;
export function start() {
  timer = setInterval(poll, 1000);
}
export function stop() {
  clearInterval(timer);
}
setInterval(heartbeat, 5000);
```

## Case: a timer cleared only on success

covers: detect:not-in-finally

```typescript path=src/call.ts
export async function call() {
  const t = setTimeout(abort, 1000);  // CANDIDATE
  await work();
  clearTimeout(t);
}
export async function safe() {
  const t = setTimeout(abort, 1000);
  try { await work(); } finally { clearTimeout(t); }
}
```

## Case: a listener added in a function that runs many times

covers: detect:listener

```typescript path=src/events.ts
export function watch(emitter) {
  emitter.on('change', () => refresh());  // CANDIDATE
}
export function watchOnce(emitter) {
  emitter.once('change', () => refresh());
}
export function watchRemovable(emitter) {
  const h = () => refresh();
  emitter.on('change', h);
  return () => emitter.off('change', h);
}
```

## Case: registration once, at start-up or module level (Do not flag)

covers: do-not-flag:registered-once

```typescript path=src/boot.ts
emitter.on('change', () => refresh());
export function init(emitter) {
  emitter.on('change', () => refresh());
}
```
