# typescript/fetch-without-error-handling: Network call with no error handling

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | ERROR_HANDLING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | typescript-review: Errors and UX |

## Intent
A failed request without a catch or status check leaves the UI stuck in a loading state, or treats an HTTP error as success.

## Detect
- `fetch(...)` result used without a `.catch`, an enclosing `try/catch`, or a check of `res.ok` / `res.status`.
- `axios.*` or an HTTP client call with no `.catch` or enclosing `try/catch`, when the awaiting function sets loading state.

## Do not flag
- A call whose caller documents handling (a shared wrapper such as `apiClient` that checks `ok`).
- Calls in a function that returns the promise to its caller.

## Examples
Flag:
```ts
const res = await fetch(url);
setData(await res.json());
```
OK:
```ts
const res = await fetch(url);
if (!res.ok) throw new Error(`HTTP ${res.status}`);
```

## Implementation notes
`fetch` does not reject on HTTP errors, so check both the `ok` test and the catch.

Detected on `fetch(...)` and `axios.*(...)`. A call is skipped when it is inside the `try` block of a `try` statement, has `.catch` on its chain, or is returned (or is an arrow function's expression body). A `fetch` is also skipped when its function mentions `ok` or `status` anywhere, which is how a status check is recognised. An `axios` call is flagged only when its function calls a `setXLoading`-style setter.

## Required tests
Missing both (finding), `ok` check (no finding), `try/catch` plus `ok` (no finding), returned promise (no finding).
