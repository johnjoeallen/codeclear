# typescript/finally-overrides-result: return or throw in finally replaces the original outcome

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | linter |
| Source | node-review: Exceptions, resources, and shutdown |

## Intent
A `return`, `throw`, `break` or `continue` inside `finally` overrides the try/catch result and discards a pending exception. Cleanup that can throw likewise masks the original error.

## Detect
- A `return`, `throw`, `break` or `continue` statement that exits a `finally` block.
- In `finally`, an awaited or sync cleanup call (`close()`, `release()`, `rollback()`) that is not wrapped in its own `try/catch`, when the `try` has a `catch` that rethrows.

## Do not flag
- Control statements in a nested function or loop fully contained in `finally`.
- Cleanup wrapped in try/catch that preserves the original error.

## Examples
Flag:
```ts
try { return await work(); } finally { return null; }
```
OK:
```ts
try { return await work(); } finally { await conn.release(); }
```

## Implementation notes
Equivalent: ESLint `no-unsafe-finally`. Emit that config for the first condition; the second needs AST.

## Required tests
`return` in finally (finding), `throw` in finally (finding), plain cleanup (no finding), jump inside nested function (no finding).

## Why deterministic
Both conditions are syntactic properties of the `finally` block.
