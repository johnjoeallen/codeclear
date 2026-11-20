# Tests: typescript/unstable-pagination-order

A `// FLAG` marker ends each line that must produce a finding.

## Case: a page with no order (Examples: Flag)

covers: example:flag,required:no-order

```typescript path=src/repo.ts
const rows = await repo.find({ where: { active: true }, take: 20, skip: 40 });  // FLAG
```

## Case: ordered by a unique key (Examples: OK)

covers: example:ok,do-not-flag:unique-order,required:unique

```typescript path=src/repo.ts
const rows = await repo.find({ take: 20, skip: 40, order: { id: 'ASC' } });
const a = await prisma.user.findMany({ take: 10, orderBy: [{ createdAt: 'desc' }, { id: 'asc' }] });
```

## Case: ordered by a column that is not unique

covers: detect:weak-order,required:weak

```typescript path=src/repo.ts
const a = await prisma.user.findMany({ take: 10, orderBy: { createdAt: 'desc' } });  // FLAG
const b = await repo.find({ limit: 10, order: { name: 'ASC' } });  // FLAG
```

## Case: chained queries

covers: detect:chained

```typescript path=src/repo.ts
const a = await knex('t').where(w).limit(10);  // FLAG
const b = await knex('t').orderBy('id').limit(10);
const c = await Model.find().sort({ _id: 1 }).limit(10);
```

## Case: limit 1 (Do not flag)

covers: do-not-flag:single,required:limit-one

```typescript path=src/repo.ts
const a = await repo.find({ where: { id }, take: 1 });
const b = await knex('t').where(w).limit(1);
```

## Case: raw SQL

covers: detect:sql

```typescript path=src/repo.ts
const a = await db.query('SELECT * FROM t WHERE x = $1 LIMIT 20');  // FLAG
const b = await db.query('SELECT * FROM t ORDER BY id LIMIT 20');
const c = await db.query('SELECT * FROM t ORDER BY created_at LIMIT 20');  // FLAG
const d = await db.query('SELECT * FROM t ORDER BY created_at, id LIMIT 20');
```

## Case: no paging at all, and a spread of options

covers: detect:no-paging

```typescript path=src/repo.ts
const a = await repo.find({ where: { active: true } });
const b = await repo.find({ take: 10, ...order });
```
