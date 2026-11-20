# typescript/pagination-iteration-defects: Pagination, batching or iteration skips, duplicates or truncates

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Values, serialization, and contracts |

## Intent
Loops that follow page tokens, batch slices, mutate a collection while iterating, or reuse a consumed iterator can lose or repeat items.

## Why AI
Completeness depends on the API's continuation semantics and what the caller promises.

## AI prompt
"Does this loop process every item exactly once? Check the continuation token handling, last partial batch, off-by-one, removal during iteration and reuse of an exhausted iterator or stream. Give the input that skips or duplicates." Evidence: loop, API/pagination contract, caller expectation.

## Do not flag
- Callers that explicitly want only the first page.
- Iteration over a copy.

## Examples
Flag:
```ts
const page = await list({ limit: 100 }); process(page.items); // ignores page.nextToken
```
OK:
```ts
do { page = await list({ token }); process(page.items); token = page.nextToken; } while (token);
```

## Evaluation cases
1. Single call to a paginated API used as the full set: flag.
2. `arr.splice(i,1)` inside `forEach` over the same array: flag.
3. Loop follows `nextToken` until empty: ok.
4. Batch loop `i += size` with `slice(i, i+size)`: ok.
