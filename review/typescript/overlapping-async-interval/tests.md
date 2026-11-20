# Tests: typescript/overlapping-async-interval

A `// FLAG` marker ends each line that must produce a finding.

## Case: an async callback (Examples: Flag)

covers: example:flag,required:async-callback

```typescript path=src/jobs.ts
setInterval(async () => { await syncAll(); }, 1000);  // FLAG
```

## Case: a recursive setTimeout (Examples: OK)

covers: example:ok,do-not-flag:recursive-timeout,required:recursive-timeout

```typescript path=src/jobs.ts
const loop = async () => { try { await syncAll(); } finally { setTimeout(loop, 1000); } };
loop();
```

## Case: an async function expression, and a callback that awaits

covers: detect:await

```typescript path=src/jobs.ts
setInterval(async function () { work(); }, 1000);  // FLAG
setInterval(() => { const r = foo(); }, 1000);
setInterval(function () { sync(); }, 1000);
```

## Case: a running flag (Do not flag)

covers: do-not-flag:guard,required:guard

```typescript path=src/jobs.ts
let running = false;
setInterval(async () => {
  if (running) return;
  running = true;
  try { await syncAll(); } finally { running = false; }
}, 1000);
```

## Case: only synchronous work (Do not flag)

covers: do-not-flag:sync,required:sync

```typescript path=src/jobs.ts
setInterval(() => { tick(); }, 1000);
```

## Case: a callback passed by name is not followed

covers: detect:named

```typescript path=src/jobs.ts
setInterval(syncAll, 1000);
```

## Case: window.setInterval

covers: detect:window

```typescript path=src/jobs.ts
window.setInterval(async () => { await poll(); }, 1000);  // FLAG
```
