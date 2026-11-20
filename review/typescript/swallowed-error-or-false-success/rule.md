# typescript/swallowed-error-or-false-success: Catch or promise chain converts failure into success

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Exceptions, resources, and shutdown; node-bug-hunter: Async completion and error ownership |

## Intent
A `catch` or `.catch(...)` that returns an ordinary value (or nothing) resolves the chain, so callers mistake a failed operation for success. Safe recovery is different from swallowing.

## Detect
- `catch {}` / `catch (e) {}` with an empty body or only a comment.
- `catch` whose body only logs and then continues or returns a success-shaped value (`return true`, `return []`, `return {}`) in a function whose callers use the result.
- `.catch(() => undefined | null | [] | {} | false)` and `.then(ok, () => ok-value)`.

## Do not flag
- Catches that rethrow, wrap with `cause`, or classify and surface the error.
- Broad boundary catches that map errors to responses correctly.
- Cleanup-only best-effort code (`close()` in `finally`).

## Examples
Flag:
```ts
const user = await getUser(id).catch(() => null); // callers treat null as "not found"
```
OK:
```ts
try { return await getUser(id); } catch (e) { if (isNotFound(e)) return null; throw e; }
```

## Implementation notes
Equivalent: `no-empty`, `@typescript-eslint/no-floating-promises`. Anchor to the catch.

Detected on a `catch` with an empty body; a `catch` whose statements are only log calls and `return true/false/[]/{}/null/undefined` (and no `throw`, and no mention of `res`, `reply`, `next`, `ctx` or a callback); `.catch(() => <literal>)`; and `.then(ok, () => <literal>)`. A `catch` inside a `finally` block is treated as best-effort cleanup.

## Required tests
Empty catch (finding), `.catch(() => [])` (finding), rethrow (no finding), not-found classification (no finding).

## AI prompt
"Does the caller now confuse this failure with a legitimate result, or lose a failure it must act on? Name the caller and the wrong outcome." Evidence: function, its callers, expected error contract.

## Why hybrid
Patterns are syntactic; whether recovery is safe depends on caller semantics.
