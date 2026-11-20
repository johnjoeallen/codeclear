# typescript/allsettled-ignored-rejections: Promise.allSettled results not classified

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: Promises, cancellation, and event-loop safety |

## Intent
`Promise.allSettled` never rejects. If the result is awaited and discarded, or only fulfilled values are read, failures vanish and the caller reports success.

## Detect
`await Promise.allSettled(...)` whose result is not assigned, or is assigned to a variable that is never examined for `status === 'rejected'` / `.reason` (no filter, loop or property access on `status`).

## Do not flag
- Results mapped with a branch on `status`.
- Cleanup code where failure is explicitly ignorable (call inside a function named `cleanup`/`dispose`).

## Examples
Flag:
```ts
await Promise.allSettled(tasks.map(run));
return { ok: true };
```
OK:
```ts
const r = await Promise.allSettled(tasks.map(run));
const failed = r.filter((x) => x.status === 'rejected');
```

## Implementation notes
Anchor to the `allSettled` call. Single-file dataflow only.

A `Promise.allSettled(...)` statement whose result is dropped is flagged. When the result goes into a variable, it is flagged if the variable is never used again or if the enclosing function never mentions `status` or `reason`; where it is read is not followed. A result that is returned or passed on is not flagged. A function whose name contains `cleanup`, `dispose`, `teardown` or `shutdown` is skipped.

## Required tests
Discarded result (finding), result never inspected (finding), filter on `status` (no finding), `.reason` read (no finding).

## Why deterministic
Presence of a status/reason check on the assigned variable is a local syntactic fact.
