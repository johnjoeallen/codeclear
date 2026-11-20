# typescript/cache-stampede-negative-caching: Cache stampede and negative caching

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Caches and distributed locks |

## Intent
When a hot key expires, many concurrent misses recompute the same expensive value. Absent values that are not cached (or failures that are cached) hammer or poison the backing store.

## Why AI
Impact depends on request concurrency, recompute cost and backing-store capacity.

## AI prompt
"For a hot key with N concurrent misses, what load hits the backend, and does it exceed capacity? Does a missing-value or error result get cached for too long (poisoning) or not at all (repeated expensive misses)? Report only with a realistic concurrent scenario and concrete capacity or correctness failure." Evidence: cache-aside code, traffic assumptions, recompute cost, TTLs.

## Do not flag
- Cheap recomputation, single-flight/request-coalescing/locks already present, low traffic.

## Examples
Flag: 500 req/s on a `homepage` key, each miss runs a 2 s aggregate, no coalescing or jitter.
OK: in-flight promise shared per key or lock-guarded refresh.

## Evaluation cases
1. Expensive aggregate, no coalescing, hot key: flag.
2. Error response cached for 1 h: flag.
3. Cheap lookup, rarely missed: ok.
