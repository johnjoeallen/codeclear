# typescript/error-cause-lost: Wrapped error drops the original cause

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | LOW |
| Confidence | HIGH |
| Detection type | ast |
| Source | node-review: Exceptions, resources, and shutdown |

## Intent
Throwing a new error from a `catch` without the original as `cause` (or message/stack) hides the root problem during diagnosis.

## Detect
Inside a `catch (e)` block: `throw new X(...)` (or `reject(new X(...))`) whose arguments do not reference `e` (no `{ cause: e }`, no `e.message`, no `e` passed to a custom error constructor).

## Do not flag
- Rethrow of `e` itself, errors that interpolate `e.message` or pass `e` as an argument.
- Intentional hiding of internals at a security boundary, when `e` is logged in the same catch.

## Examples
Flag:
```ts
try { await db.query(q); } catch (e) { throw new Error('query failed'); }
```
OK:
```ts
catch (e) { throw new Error('query failed', { cause: e }); }
```

## Implementation notes
Equivalent: `preserve-caught-error` (ESLint 9.35+), `unicorn/prefer-error-cause`. Anchor to the `throw`.

Detected on `new X(...)` that is thrown, or passed to `reject(...)`, inside a `catch` block and not inside a function declared in it. The new error counts as using the caught one when it mentions the catch parameter anywhere; a catch that logs with `console.*` or `logger.*` is treated as a deliberate security boundary and skipped. A catch with no parameter (`catch {`) never mentions one.

## Required tests
New error without `e` (finding), `{ cause: e }` (no finding), `e.message` interpolated (no finding), log then throw generic (no finding).

## Why deterministic
Whether the caught binding is referenced by the thrown expression is a syntactic check.
