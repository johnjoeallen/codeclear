# Tests: typescript/sql-injection-dynamic-query

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a template literal with a value (Examples: Flag)

covers: example:flag,required:template

```typescript path=src/db.ts
export function find(db, name: string) {
  return db.query(`SELECT * FROM users WHERE name = '${name}'`);  // CANDIDATE
}
```

## Case: parameters and tagged templates (Examples: OK)

covers: example:ok,do-not-flag:parameterized,required:parameters

```typescript path=src/db.ts
export function a(db, name: string) {
  return db.query('SELECT * FROM users WHERE name = $1', [name]);
}
export function b(prisma, name: string) {
  return prisma.$queryRaw`SELECT * FROM users WHERE name = ${name}`;
}
export function c(sql, name: string) {
  return sql`SELECT * FROM users WHERE name = ${name}`;
}
```

## Case: concatenation and the other methods

covers: detect:forms

```typescript path=src/db.ts
export function a(db, id: string, col: string, dir: string) {
  db.query("DELETE FROM t WHERE id = " + id);  // CANDIDATE
  db.execute(`UPDATE t SET a = 1 WHERE id = ${id}`);  // CANDIDATE
  knex.raw(`SELECT * FROM t ORDER BY ${col} ${dir}`);  // CANDIDATE
  prisma.$queryRawUnsafe(`SELECT * FROM t WHERE id = ${id}`);  // CANDIDATE
}
```

## Case: an allow-listed column (Do not flag)

covers: do-not-flag:allow-listed-identifier

```typescript path=src/db.ts
const ALLOWED = ['name', 'created_at'];
export function a(db, col: string) {
  if (!ALLOWED.includes(col)) throw new Error('bad column');
  return db.query(`SELECT * FROM users ORDER BY ${col}`);
}
```

## Case: strings that are not SQL, and constant queries

covers: detect:not-sql

```typescript path=src/db.ts
export function a(db, name: string, cache) {
  db.query('SELECT 1');
  cache.get(`user:${name}`);
  log.query(`hello ${name}`);
}
```
