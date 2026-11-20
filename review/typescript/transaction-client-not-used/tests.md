# Tests: typescript/transaction-client-not-used

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: the outer client inside a transaction (Examples: Flag)

covers: example:flag,required:outer-client

```typescript path=src/orders.ts
export async function create(order) {
  await prisma.$transaction(async (tx) => {
    await tx.order.create({ data: order });
    await prisma.audit.create({ data: { order: order.id } });  // CANDIDATE
  });
}
```

## Case: the handle the callback receives (Examples: OK)

covers: example:ok,do-not-flag:uses-handle,required:uses-tx

```typescript path=src/orders.ts
export async function create(order) {
  await prisma.$transaction(async (tx) => {
    await tx.order.create({ data: order });
    await tx.audit.create({ data: { order: order.id } });
  });
}
```

## Case: knex, Sequelize and TypeORM

covers: detect:other-orms

```typescript path=src/orders.ts
export async function a() {
  await knex.transaction(async (trx) => {
    await trx('orders').insert({});
    await knex('audit').insert({});  // CANDIDATE
  });
  await sequelize.transaction(async (t) => {
    await Order.create({}, { transaction: t });
    await Audit.create({});  // CANDIDATE
  });
  await dataSource.transaction(async (manager) => {
    await manager.save(order);
    await orderRepo.save(audit);  // CANDIDATE
  });
}
```

## Case: the handle passed in the arguments (Do not flag)

covers: do-not-flag:handle-in-args

```typescript path=src/orders.ts
export async function a() {
  await sequelize.transaction(async (t) => {
    await Order.create({}, { transaction: t });
    await Order.update({}, { where: {}, transaction: t });
  });
}
```

## Case: calls outside any transaction

covers: detect:no-transaction

```typescript path=src/orders.ts
export async function a() {
  await prisma.audit.create({ data: {} });
}
```
