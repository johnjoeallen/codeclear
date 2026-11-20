# Tests: typescript/loading-state-never-cleared

A `// FLAG` marker ends each line that must produce a finding.

## Case: set to true and only cleared on success (Examples: Flag)

covers: example:flag,required:success-only

```typescript path=src/load.ts
async function load() {
  setLoading(true);  // FLAG
  const r = await fetchAll();
  setData(r);
  setLoading(false);
}
```

## Case: cleared in finally (Examples: OK)

covers: example:ok,do-not-flag:finally,required:finally

```typescript path=src/load.ts
async function load() {
  setLoading(true);
  try { setData(await fetchAll()); } finally { setLoading(false); }
}
async function other() {
  setSubmitting(true);
  await save().finally(() => setSubmitting(false));
}
```

## Case: cleared on both the success and the failure path (Do not flag)

covers: do-not-flag:both-paths

```typescript path=src/load.ts
async function load() {
  setBusy(true);
  try { await go(); setBusy(false); } catch (e) { setBusy(false); }
}
function then() {
  setFetching(true);
  fetchAll().then((r) => { setData(r); setFetching(false); }).catch(() => setFetching(false));
}
```

## Case: cleared only on success inside a try with a catch that does not clear

covers: detect:catch-missing

```typescript path=src/load.ts
async function load() {
  setPending(true);  // FLAG
  try { await go(); setPending(false); } catch (e) { log(e); }
}
```

## Case: .then without a catch

covers: detect:then

```typescript path=src/load.ts
function load() {
  setIsLoading(true);  // FLAG
  fetchAll().then((r) => { setData(r); setIsLoading(false); });
}
```

## Case: nothing is awaited, and an unrelated setter

covers: detect:no-await

```typescript path=src/load.ts
function a() {
  setLoading(true);
  setData(compute());
}
async function b() {
  setCount(true);
  await go();
}
```
