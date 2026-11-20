# typescript/unstable-pagination-order: Paginated query without a deterministic ordering

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | node-review: Databases, ORM, and transactions |

## Intent
`LIMIT/OFFSET` or cursor pagination without a unique, stable sort can skip or repeat rows between pages.

## Detect
A call with `limit`/`take`/`offset`/`skip`/`LIMIT` and no `orderBy`/`order`/`sort`/`ORDER BY`; or an ordering by a non-unique column only (`createdAt`, `status`, `name`) with no unique tiebreaker (`id`).

## Do not flag
- Queries ordered by a unique key, or including a unique tiebreaker.
- `limit 1`, and `findFirst` with an intended arbitrary pick.

## Examples
Flag:
```ts
Post.findAll({ limit: 20, offset: page * 20 });
```
OK:
```ts
Post.findAll({ limit: 20, offset: page * 20, order: [['createdAt', 'DESC'], ['id', 'DESC']] });
```

## Implementation notes
Anchor to the query call. Raw SQL: regex on the literal.

Detected on calls with an options object that has `take`, `limit`, `skip` or `offset` and no `order`, `orderBy` or `sort` (a spread is trusted to hold one), on chained `.limit(n)`/`.take(n)`/`.skip(n)`/`.offset(n)` with no ordering call in the same statement, and on SQL strings with `LIMIT` and no `ORDER BY`. `limit 1`/`take: 1` is skipped. An order that names only `createdAt`, `updatedAt`, `status` or `name` without `id` is flagged as a weak order.

## Required tests
No order (finding), order by `createdAt` only (finding), with `id` tiebreaker (no finding), `limit 1` (no finding).

## Why deterministic
Presence of a limit/offset and the shape of the order clause are syntactic; uniqueness is judged by the column name list (`id`, `uuid`, primary key).
