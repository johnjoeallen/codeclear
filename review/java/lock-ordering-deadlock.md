# java/lock-ordering-deadlock: Inconsistent lock acquisition order

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
Two code paths acquiring the same locks in opposite order can deadlock; coarse locks can also serialise more work than intended.

## Why AI
A deadlock needs two opposing acquisition paths, which usually span methods or classes; reporting without both paths is speculation.

## AI prompt
"List every lock/monitor acquired in this change and the order. Find another path in the supplied code acquiring any two of them in the opposite order. Report only with both paths (method names, order) and a deadlock interleaving. Separately, flag a lock that serialises unrelated work only if a concrete throughput consequence is shown." Evidence: changed class, all methods using the same locks, callers across classes.

## Do not flag
- Single lock only, or locks always taken in a documented global order, or `tryLock` with timeout and back-off.
- Nested locks where the opposing path is not shown.

## Examples
Flag: `transfer(a,b)` locks `a` then `b`; `transfer(b,a)` locks `b` then `a`.
OK: locks ordered by account id before acquisition.

## Evaluation cases
1. Two methods taking `lockA` then `lockB` and `lockB` then `lockA` - report.
2. Same order in both - no report.
3. Nested synchronized, no other path shown - no report.
4. Sorted-lock acquisition helper used by all paths - no report.
