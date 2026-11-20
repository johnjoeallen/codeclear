# Tests: typescript/transaction-commit-rollback-paths

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: BEGIN with no ROLLBACK (Examples: Flag)

covers: example:flag,required:no-rollback

```typescript path=src/pay.ts
export async function pay(client, order) {  // CANDIDATE
  await client.query('BEGIN');
  await client.query('insert into orders values ($1)', [order]);
  await client.query('COMMIT');
}
```

## Case: ROLLBACK and release in finally (Examples: OK)

covers: example:ok,do-not-flag:managed-and-rethrown,required:rollback

```typescript path=src/pay.ts
export async function pay(pool, order) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query('insert into orders values ($1)', [order]);
    await client.query('COMMIT');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}
```

## Case: a connection that is not released

covers: detect:release

```typescript path=src/pay.ts
export async function a(pool) {  // CANDIDATE
  const client = await pool.connect();
  await client.query('select 1');
  client.release();
}
```

## Case: a catch that swallows inside a transaction callback

covers: detect:swallowing-catch

```typescript path=src/pay.ts
export async function a(prisma) {
  await prisma.$transaction(async (tx) => {
    try {  // CANDIDATE
      await tx.order.create({});
    } catch (e) {
      logger.error(e);
    }
  });
}
export async function b(prisma) {
  await prisma.$transaction(async (tx) => {
    try {
      await tx.order.create({});
    } catch (e) {
      throw e;
    }
  });
}
```

## Case: commit not awaited

covers: detect:commit

```typescript path=src/pay.ts
export async function a(t) {
  t.commit();  // CANDIDATE
}
export async function b(t) {
  await t.commit();
}
```

## Case: a catch that rethrows after cleanup (Do not flag)

covers: do-not-flag:rethrow-after-cleanup

```typescript path=src/pay.ts
export async function a(db) {
  await db.transaction(async (trx) => {
    try {
      await trx('t').insert({});
    } catch (e) {
      await cleanup();
      throw e;
    }
  });
}
```
