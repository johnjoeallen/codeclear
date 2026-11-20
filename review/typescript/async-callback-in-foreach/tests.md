# Tests: typescript/async-callback-in-foreach

A `// FLAG` marker ends each line that must produce a finding.

## Case: an async arrow function (Examples: Flag)

covers: example:flag,required:async-arrow

```typescript path=src/a.ts
items.forEach(async (i) => {  // FLAG
  await save(i);
});
```

## Case: for...of with await (Examples: OK)

covers: example:ok,do-not-flag:for-of,required:for-of

```typescript path=src/a.ts
for (const i of items) {
  await save(i);
}
for await (const i of stream) {
  use(i);
}
```

## Case: an async function expression

covers: detect:function-expression

```typescript path=src/a.ts
items.forEach(async function (i) {  // FLAG
  await save(i);
});
```

## Case: map with Promise.all, and a sync callback (Do not flag)

covers: do-not-flag:map-promise-all,required:map

```typescript path=src/a.ts
await Promise.all(items.map(async (i) => save(i)));
items.forEach((i) => save(i));
items.forEach(handler);
```

## Case: a callback that is not the first argument is not a forEach callback

covers: detect:first-argument

```typescript path=src/a.ts
items.forEach(console.log, async () => 1);
```
