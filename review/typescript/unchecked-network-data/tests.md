# Tests: typescript/unchecked-network-data

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an assertion on a response body (Examples: Flag)

covers: example:flag,required:as-json

```typescript path=src/api.ts
export async function load() {
  const res = await fetch('/api/user');
  const user = (await res.json()) as User;  // CANDIDATE
  return user.name;
}
```

## Case: parsed with a schema (Examples: OK)

covers: example:ok,do-not-flag:validated,required:schema

```typescript path=src/api.ts
export async function load() {
  const res = await fetch('/api/user');
  const user = UserSchema.parse(await res.json());
  return user.name;
}
```

## Case: axios data and JSON.parse

covers: detect:sources

```typescript path=src/api.ts
export async function a() {
  const r = await axios.get('/api/x');
  return r.data as Items;  // CANDIDATE
}
export function b(text: string) {
  return JSON.parse(text) as Config;  // CANDIDATE
}
```

## Case: a non-null assertion on network data

covers: detect:non-null

```typescript path=src/api.ts
export async function a() {
  const body = await (await fetch('/api/x')).json();
  return body.items![0];  // CANDIDATE
}
```

## Case: as unknown, as const and data that is not from the network

covers: detect:harmless-assertions

```typescript path=src/api.ts
export function a(text: string, el: Element) {
  const u = JSON.parse(text) as unknown;
  const k = ['a', 'b'] as const;
  const e = el as HTMLElement;
  return [u, k, e];
}
```

## Case: assertions in test files (Do not flag)

covers: do-not-flag:test-file

```typescript path=src/api.test.ts
export async function load() {
  const res = await fetch('/api/user');
  return (await res.json()) as User;
}
```
