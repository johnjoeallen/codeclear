# typescript/async-callback-in-foreach: async callback passed to forEach

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | typescript-review: Correctness |

## Intent
`Array.prototype.forEach` ignores the promise an `async` callback returns. Code after the loop runs before the work finishes and rejections are lost.

## Detect
A call `x.forEach(cb)` where `cb` is an `async` arrow function or `async function` expression.

## Do not flag
- `map` with `async` callbacks whose result is passed to `Promise.all`, `allSettled`, `race` or `any`.
- `for await` and `for...of` loops.

## Examples
Flag:
```ts
items.forEach(async (item) => { await save(item); });
```
OK:
```ts
await Promise.all(items.map(async (item) => save(item)));
```

## Implementation notes
Equivalent: ESLint `@typescript-eslint/no-misused-promises` with `checksVoidReturn`. Prefer emitting that config. Anchor to the `forEach` call.

Detected on `x.forEach(cb)` where the first argument is an `async` arrow function or function expression. `map` is not looked at, and `for...of` and `for await` are different statements, so none of them is flagged.

## Required tests
Async forEach (finding), sync forEach (no finding), async map inside `Promise.all` (no finding), async handler in `for...of` (no finding).
