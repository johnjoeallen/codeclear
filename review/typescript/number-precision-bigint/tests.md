# Tests: typescript/number-precision-bigint

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an id from a row converted to a number (Examples: Flag)

covers: example:flag,required:number-id

```typescript path=src/users.ts
export function load(row) {
  return { id: Number(row.id), name: row.name };  // CANDIDATE
}
```

## Case: kept as a string or BigInt (Examples: OK)

covers: example:ok,do-not-flag:string-or-bigint,required:string

```typescript path=src/users.ts
export function load(row) {
  return { id: String(row.id), big: BigInt(row.id) };
}
```

## Case: parseInt, unary plus and counters

covers: detect:conversions

```typescript path=src/users.ts
export function a(row, result) {
  const x = parseInt(row.userId, 10);  // CANDIDATE
  const y = +row.balance;  // CANDIDATE
  const z = Number(result.orderCount);  // CANDIDATE
}
```

## Case: UUID ids (Do not flag)

covers: do-not-flag:uuid

```typescript path=src/uuid.ts
import { validate } from 'uuid';
export function a(row) {
  return Number(row.id);
}
```

## Case: JSON of a BigInt

covers: detect:json-bigint

```typescript path=src/json.ts
export function a(res) {
  res.json({ id: 10n });  // CANDIDATE
  JSON.stringify({ total: BigInt(5) });  // CANDIDATE
  JSON.stringify({ total: 5 });
}
```

## Case: a replacer or a toJSON override (Do not flag)

covers: do-not-flag:replacer

```typescript path=src/json2.ts
BigInt.prototype.toJSON = function () { return this.toString(); };
export function a(res) {
  res.json({ id: 10n });
}
```
