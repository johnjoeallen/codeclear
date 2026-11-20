# Tests: typescript/truthiness-drops-valid-value

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: || on a count (Examples: Flag)

covers: example:flag,required:or-fallback

```typescript path=src/page.ts
export function size(opts) {
  return opts.limit || 20;  // CANDIDATE
}
```

## Case: ?? and explicit checks (Examples: OK)

covers: example:ok,do-not-flag:nullish-and-explicit,required:nullish

```typescript path=src/page.ts
export function a(opts) {
  return opts.limit ?? 20;
}
export function b(count: number | undefined) {
  if (count === undefined) return 0;
  if (count == null) return 1;
  return count;
}
```

## Case: ! and if

covers: detect:negation

```typescript path=src/page.ts
export function a(total: number, offset: number) {
  if (!total) return;  // CANDIDATE
  if (offset) { go(); }  // CANDIDATE
}
```

## Case: a ternary

covers: detect:ternary

```typescript path=src/page.ts
export function a(quantity: number) {
  return quantity ? quantity : 1;  // CANDIDATE
}
```

## Case: names that are not scalars (Do not flag)

covers: do-not-flag:objects-and-lists

```typescript path=src/page.ts
export function a(user, items, options, name: string) {
  if (!user) return;
  if (items) { go(); }
  return options || {};
}
export function b(itemCount: number | null) {
  return name || 'x';
}
```

## Case: zero is invalid and validated elsewhere is left to the AI (Do not flag)

covers: do-not-flag:zero-invalid

```typescript path=src/validated.ts
export function a(amount: number) {
  if (!amount) throw new Error('amount required');  // CANDIDATE
}
```
