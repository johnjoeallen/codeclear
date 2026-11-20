# python/iteration-and-pagination-boundaries: Exhausted generator, mutation during iteration, pagination or batch boundary errors

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow |

## Intent
Iterating a generator twice, mutating a collection while iterating it, or paging/batching with an off-by-one or unstable order skips, duplicates or misroutes work.

## Why AI
Whether a generator is reused, whether the order key is unique and stable, and whether the boundary matters depends on the data source and surrounding logic.

## AI prompt
"Does this loop, generator use, or pagination/batching logic skip, duplicate or misroute items under a concrete input?" Supply the changed code, the query/ordering and page-size source, and the consumers of each item. Report only with a concrete trigger scenario (input, dropped/duplicated item, changed line); otherwise NO_FINDING.

## Do not flag
- Iteration over a copy (`list(d)`), or mutation of values that does not change the container size.
- Pagination ordered by a unique, immutable key with inclusive/exclusive bounds handled correctly.

## Examples
Flag:
```python
rows = (r for r in fetch())
total = sum(1 for _ in rows)
for r in rows: process(r)      # already exhausted
```
OK:
```python
rows = list(fetch())
```

## Evaluation cases
1. `for k in d: if bad(k): del d[k]` -> flag.
2. `OFFSET` paging ordered by non-unique `created_at` while rows are inserted -> flag.
3. `for s in range(0, len(ids), 100): batch = ids[s:s+100]` -> no finding.
4. Generator passed to two consumers sequentially -> flag.
