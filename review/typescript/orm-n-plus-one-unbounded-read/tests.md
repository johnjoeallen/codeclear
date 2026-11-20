# Tests: typescript/orm-n-plus-one-unbounded-read

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a query in a loop (Examples: Flag)

covers: example:flag,required:loop-query

```typescript path=src/orders.ts
export async function load(orders) {
  for (const o of orders) {
    o.items = await prisma.item.findMany({ where: { orderId: o.id } });  // CANDIDATE
  }
}
```

## Case: one query for all (Examples: OK)

covers: example:ok,do-not-flag:batched,required:batched

```typescript path=src/orders.ts
export async function load(orders) {
  const ids = orders.map((o) => o.id);
  return prisma.item.findMany({ where: { orderId: { in: ids } }, take: 100 });
}
```

## Case: a query in a map callback

covers: detect:map

```typescript path=src/orders.ts
export async function load(users) {
  return Promise.all(users.map(async (u) => {
    const o = await orderRepo.findOne({ where: { user: u.id } });  // CANDIDATE
    return o;
  }));
}
```

## Case: reading every row on a request path

covers: detect:unbounded

```typescript path=src/api.ts
export async function list(req, res) {
  const all = await prisma.user.findMany();  // CANDIDATE
  const some = await prisma.user.findMany({ take: 20 });
  res.json(all);
}
```

## Case: a client made per request

covers: detect:client-per-call

```typescript path=src/api.ts
export async function handler(req, res) {
  const prisma = new PrismaClient();  // CANDIDATE
  res.json(await prisma.user.count());
}
export function createDb() {
  return new Pool();
}
```

## Case: limits, cursors and one-time scripts (Do not flag)

covers: do-not-flag:limits-and-scripts

```typescript path=src/paged.ts
export async function page(req, res) {
  const rows = await prisma.user.findMany({ take: 50, cursor: { id: req.query.after } });
  res.json(rows);
}
```

```typescript path=scripts/backfill.ts
export async function run(orders) {
  for (const o of orders) {
    await prisma.item.findMany({ where: { orderId: o.id } });
  }
}
```
