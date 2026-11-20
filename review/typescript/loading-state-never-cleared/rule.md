# typescript/loading-state-never-cleared: Loading flag not reset when the request fails

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | ERROR_HANDLING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | typescript-review: Errors and UX |

## Intent
If `setLoading(true)` is not paired with a reset on the failure path, the UI spinner never ends after a rejected request.

## Detect
In one function: a call `setX(true)` where `X` matches `loading|busy|pending|submitting|fetching`, followed by an `await` or `.then`, and no `setX(false)` in a `finally` block or in both the success and `catch` paths.

## Do not flag
- Reset in `finally`.
- Data-fetching libraries that manage the flag (`useQuery`, `useSWR`).

## Examples
Flag:
```ts
setLoading(true);
const res = await api.get();
setData(res); setLoading(false);
```
OK:
```ts
setLoading(true);
try { setData(await api.get()); } finally { setLoading(false); }
```

## Implementation notes
Anchor to the `setX(true)` call.

Detected on `setXxx(true)` where the name contains `loading`, `busy`, `pending`, `submitting` or `fetching`, in a function that later awaits or calls `.then`. It is cleared when the same setter is called with `false` in a `finally` block or a `.finally(...)`, or both in a `catch` (or `.catch(...)`) and somewhere on the success path. Libraries that manage the flag (`useQuery`, `useSWR`) never call such a setter, so they are not seen.

## Required tests
No reset on failure (finding), `finally` reset (no finding), reset in both branches (no finding), `useQuery` (no finding).

## Why deterministic
Matching a set-true call to a reset on all exit paths is control-flow structure in one function.
