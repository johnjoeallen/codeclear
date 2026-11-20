# Tests: typescript/allsettled-ignored-rejections

A `// FLAG` marker ends each line that must produce a finding.

## Case: the result is dropped (Examples: Flag)

covers: example:flag,required:dropped

```typescript path=src/run.ts
async function f() {
  await Promise.allSettled(tasks.map(run));  // FLAG
  return { ok: true };
}
```

## Case: results are filtered on status (Examples: OK)

covers: example:ok,do-not-flag:status-checked,required:status

```typescript path=src/run.ts
async function f() {
  const r = await Promise.allSettled(tasks.map(run));
  const failed = r.filter((x) => x.status === 'rejected');
  return failed;
}
```

## Case: assigned and never read, and read without status

covers: detect:variable

```typescript path=src/run.ts
async function f() {
  const unused = await Promise.allSettled(tasks);  // FLAG
}
async function g() {
  const r = await Promise.allSettled(tasks);  // FLAG
  return r.length;
}
```

## Case: a call without await, dropped

covers: detect:no-await

```typescript path=src/run.ts
function f() {
  Promise.allSettled(tasks);  // FLAG
}
```

## Case: cleanup code (Do not flag)

covers: do-not-flag:cleanup,required:cleanup

```typescript path=src/run.ts
async function cleanup() {
  await Promise.allSettled(handles.map(close));
}
const dispose = async () => { await Promise.allSettled(a); };
```

## Case: a returned or passed-on result

covers: detect:returned

```typescript path=src/run.ts
async function f() {
  return Promise.allSettled(tasks);
}
async function g() {
  report(await Promise.allSettled(tasks));
}
```

## Case: a reason read in a loop

covers: detect:reason

```typescript path=src/run.ts
async function f() {
  const r = await Promise.allSettled(tasks);
  for (const x of r) { if (x.status === 'rejected') log(x.reason); }
}
```
