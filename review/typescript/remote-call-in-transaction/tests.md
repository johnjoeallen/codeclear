# Tests: typescript/remote-call-in-transaction

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an HTTP call inside a transaction (Examples: Flag)

covers: example:flag,required:in-transaction

```typescript path=src/orders.ts
export async function pay(order) {
  await db.transaction(async (trx) => {
    await trx('orders').insert(order);
    await fetch(GATEWAY, { method: 'POST' });  // CANDIDATE
  });
}
```

## Case: the call after the commit (Examples: OK)

covers: example:ok,do-not-flag:after-commit,required:after-commit

```typescript path=src/orders.ts
export async function pay(order) {
  await db.transaction(async (trx) => {
    await trx('orders').insert(order);
  });
  await fetch(GATEWAY, { method: 'POST' });
}
```

## Case: other transaction forms and remote calls

covers: detect:forms

```typescript path=src/orders.ts
export async function a(prisma, order) {
  await prisma.$transaction(async (tx) => {
    await tx.order.create({ data: order });
    await axios.post(URL, order);  // CANDIDATE
    await producer.send({ topic: 't', messages: [] });  // CANDIDATE
    await transporter.sendMail(mail);  // CANDIDATE
  });
}
```

## Case: calls to the same database (Do not flag)

covers: do-not-flag:same-database

```typescript path=src/orders.ts
export async function a(db) {
  await db.transaction(async (trx) => {
    await trx('orders').insert({});
    await trx.raw('select 1');
    helper();
  });
}
```

## Case: an outbox write and an afterCommit hook (Do not flag)

covers: do-not-flag:outbox

```typescript path=src/orders.ts
export async function a(db, order) {
  await db.transaction(async (trx) => {
    await trx('orders').insert(order);
    trx.afterCommit(() => fetch(URL));
  });
}
```
