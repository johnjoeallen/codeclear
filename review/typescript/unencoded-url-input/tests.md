# Tests: typescript/unencoded-url-input

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a parameter in a URL (Examples: Flag)

covers: example:flag,required:template

```typescript path=src/api.ts
export function user(name: string) {
  return fetch(`/api/users/${name}`);  // CANDIDATE
}
```

## Case: encodeURIComponent and URLSearchParams (Examples: OK)

covers: example:ok,do-not-flag:encoded,required:encoded

```typescript path=src/api.ts
export function a(name: string) {
  return fetch(`/api/users/${encodeURIComponent(name)}`);
}
export function b(q: string) {
  const params = new URLSearchParams({ q });
  return fetch('/api/search?' + params.toString());
}
```

## Case: concatenation, axios and request values

covers: detect:forms

```typescript path=src/api.ts
export function a(name: string, req, location) {
  fetch('/api/users/' + name);  // CANDIDATE
  axios.get(`/api/items?q=${req.query.q}`);  // CANDIDATE
  window.open(`/go?to=${location.hash}`);  // CANDIDATE
}
```

## Case: constants and numbers (Do not flag)

covers: do-not-flag:constants-and-numbers

```typescript path=src/api.ts
const BASE = '/api';
export function a(page: number, limit: number) {
  fetch(`${BASE}/items`);
  fetch(`/api/items?page=${page}&limit=${limit}`);
  fetch('/api/health');
}
```

## Case: a value that is not user input

covers: detect:not-user-input

```typescript path=src/api.ts
export function a() {
  const id = computeId();
  return fetch(`/api/items/${id}`);
}
```
