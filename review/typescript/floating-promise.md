# typescript/floating-promise: Promise neither awaited nor handled

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | linter |
| Source | typescript-review: Correctness |

## Intent
A promise that is not awaited, returned, or given a `.catch` loses its failure and may finish after the caller moved on.

## Detect
An expression statement whose type is `Promise` and that is not awaited, returned, assigned, passed on, `void`-prefixed, or followed by `.catch(...)` / `.then(_, onRejected)`.

## Do not flag
- Expressions explicitly prefixed with `void` where the project allows it.
- Fire-and-forget calls in `main` entry files that attach a global `unhandledRejection` handler.

## Examples
Flag:
```ts
saveUser(user);
```
OK:
```ts
await saveUser(user);
void saveUser(user).catch(report);
```

## Implementation notes
Needs type information, so use the TypeScript checker. Emit ESLint `@typescript-eslint/no-floating-promises` rather than custom code. Plain `.js` files have no types, so skip them.

## Required tests
Bare call (finding), awaited (no finding), `.catch` attached (no finding), `void` (no finding), returned (no finding).
