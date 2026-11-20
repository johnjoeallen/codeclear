# Tests: typescript/poisoned-memoized-promise

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a client promise kept forever (Examples: Flag)

covers: example:flag,required:memoized

```typescript path=src/client.ts
let client;
export function getClient() {
  client ??= connect();  // CANDIDATE
  return client;
}
```

## Case: reset on failure (Examples: OK)

covers: example:ok,do-not-flag:reset,required:reset

```typescript path=src/client.ts
let client;
export function getClient() {
  if (!client) {
    client = connect();
    client.catch(() => { client = undefined; });
  }
  return client;
}
```

## Case: the if form

covers: detect:if-form

```typescript path=src/client.ts
let p;
export function init() {
  if (!p) p = setup();  // CANDIDATE
  return p;
}
```

## Case: a promise with a catch, and an awaited value (Do not flag)

covers: do-not-flag:catch-or-awaited

```typescript path=src/client.ts
let a;
export function one() {
  a = connect().catch((e) => { a = undefined; throw e; });
  return a;
}
let b;
export async function two() {
  b = await connect();
  return b;
}
```

## Case: a local variable and a variable that is not at module level

covers: detect:module-level

```typescript path=src/client.ts
export function f() {
  let local;
  local = connect();
  return local;
}
```
