# typescript/orm-n-plus-one-unbounded-read: N+1 queries, unbounded reads and connection lifecycle

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions |

## Intent
A query per loop iteration, a read with no limit, lazy relation loading, or a connection created per call cause load spikes and memory exhaustion.

## Detect
- An `await` of `find*/findOne/query/get` inside a `for`/`for...of`/`map`/`forEach` over a query result.
- `findMany()`, `find({})`, `SELECT` without `LIMIT`/`take` on a request path.
- Relation property access (`order.items`) in a loop where the ORM lazy-loads.
- `new PrismaClient()`, `new Pool()`, `createConnection()` inside a request handler or function called per request.

## Do not flag
- Batched queries (`where id in`, DataLoader), loops over small constants, one-time scripts.
- Queries with limits or cursors.

## Examples
Flag:
```ts
for (const o of orders) { o.items = await Item.findAll({ where: { orderId: o.id } }); }
```
OK:
```ts
const items = await Item.findAll({ where: { orderId: orders.map((o) => o.id) } });
```

## Implementation notes
Anchor to the in-loop query or client creation.

## Required tests
Query in loop (finding), batched (no finding), `new PrismaClient` in handler (finding), module-level client (no finding), `findMany` with `take` (no finding).

## AI prompt
"With realistic data sizes, does this cause a material failure or load impact?" Evidence: loop bounds, table sizes if known, ORM loading config.

## Why hybrid
The patterns are syntactic; materiality depends on data volume.
