# Tests: typescript/fetch-without-error-handling

A `// FLAG` marker ends each line that must produce a finding.

## Case: a fetch with no handling (Examples: Flag)

covers: example:flag,required:no-handling

```typescript path=src/load.ts
async function load() {
  const res = await fetch(url);  // FLAG
  setData(await res.json());
}
```

## Case: an ok check (Examples: OK)

covers: example:ok,required:ok-check

```typescript path=src/load.ts
async function load() {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
```

## Case: try/catch and .catch

covers: detect:catch,required:try-catch

```typescript path=src/load.ts
async function a() {
  try { const res = await fetch(url); use(res); } catch (e) { log(e); }
}
function b() {
  fetch(url).then(use).catch(log);
}
```

## Case: the promise is handed back (Do not flag)

covers: do-not-flag:returned,required:returned

```typescript path=src/load.ts
function a() { return fetch(url); }
async function b() { return await fetch(url); }
const c = () => fetch(url);
```

## Case: a fetch inside the catch block is not protected by it

covers: detect:in-handler

```typescript path=src/load.ts
async function a() {
  try { work(); } catch (e) { await fetch(reportUrl); }  // FLAG
}
```

## Case: axios when the function sets loading state

covers: detect:axios

```typescript path=src/load.ts
async function a() {
  setLoading(true);
  const r = await axios.get(url);  // FLAG
  setData(r.data);
}
async function b() {
  const r = await axios.get(url);
  return r.data;
}
async function c() {
  setLoading(true);
  try { await axios.get(url); } finally { setLoading(false); }
}
```

## Case: a shared wrapper (Do not flag)

covers: do-not-flag:wrapper

```typescript path=src/load.ts
async function a() {
  const res = await apiClient.get(url);
  setData(res);
}
```

## Case: a test file

covers: detect:test-file

```typescript path=src/load.test.ts
async function a() { const res = await fetch(url); use(res); }
```
