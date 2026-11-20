# Tests: typescript/http-client-no-timeout

A `// FLAG` marker ends each line that must produce a finding.

## Case: fetch with no signal (Examples: Flag)

covers: example:flag,required:fetch

```typescript path=src/api.ts
const r = await fetch(url);  // FLAG
```

## Case: an AbortSignal timeout (Examples: OK)

covers: example:ok,do-not-flag:abort-signal,required:signal

```typescript path=src/api.ts
const r = await fetch(url, { signal: AbortSignal.timeout(3000) });
const q = await fetch(url, { method: 'POST', signal });
```

## Case: fetch with options that have no signal

covers: detect:fetch-options

```typescript path=src/api.ts
const r = await fetch(url, { method: 'POST', body });  // FLAG
const s = await fetch(url, opts);
const t = await fetch(url, { ...base });
```

## Case: axios

covers: detect:axios,required:axios

```typescript path=src/api.ts
await axios.get(url);  // FLAG
await axios.post(url, body);  // FLAG
await axios.get(url, { timeout: 5000 });
await axios.post(url, body, { timeout: 5000 });
await axios.request({ url, method: 'GET' });  // FLAG
await axios.request({ url, timeout: 5000 });
```

## Case: a client created with and without a default timeout (Do not flag)

covers: do-not-flag:default-timeout,required:axios-create

```typescript path=src/api.ts
const good = axios.create({ baseURL, timeout: 3000 });
await good.get(url);
const bad = axios.create({ baseURL });  // FLAG
```

## Case: got, http and https

covers: detect:other-clients

```typescript path=src/api.ts
await got(url);  // FLAG
await got.post(url, { json: body });  // FLAG
await got(url, { timeout: { request: 3000 } });
const req = https.request({ host }, cb);  // FLAG
const ok = https.request({ host, timeout: 3000 }, cb);
```

## Case: browser-only code (Do not flag)

covers: do-not-flag:browser-only

```tsx path=src/components/List.tsx
const r = await fetch(url);
```

```typescript path=public/app.ts
const r = await fetch(url);
```
