# Tests: typescript/float-money-arithmetic

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: arithmetic on a price (Examples: Flag)

covers: example:flag,required:arithmetic

```typescript path=src/cart.ts
export function total(items) {
  let sum = 0;
  for (const i of items) { sum = sum + i.price * i.qty; }  // CANDIDATE
  return sum;
}
```

## Case: integer cents and a decimal library (Examples: OK)

covers: example:ok,do-not-flag:integer-minor-units,required:cents

```typescript path=src/cents.ts
export function total(items) {
  let cents = 0;
  for (const i of items) { cents = cents + i.priceCents * i.qty; }
  return cents;
}
```

```typescript path=src/dec.ts
import Decimal from 'decimal.js';
export function total(items) {
  return items.reduce((s, i) => s.plus(new Decimal(i.price)), new Decimal(0));
}
```

## Case: toFixed, Math.round and parseFloat

covers: detect:rounding

```typescript path=src/fmt.ts
export function a(amount: number, balance: string) {
  const s = amount.toFixed(2);  // CANDIDATE
  const r = Math.round(amount * 100) / 100;  // CANDIDATE
  const p = parseFloat(balance);  // CANDIDATE
  return [s, r, p];
}
```

## Case: display-only formatting (Do not flag)

covers: do-not-flag:display-only

```typescript path=src/display.ts
export function show(amount: number) {
  return new Intl.NumberFormat('en', { style: 'currency', currency: 'EUR' }).format(amount + 0);
}
```

## Case: non-monetary quantities (Do not flag)

covers: do-not-flag:non-monetary

```typescript path=src/stats.ts
export function mean(values: number[]) {
  const count = values.length;
  const sum = values.reduce((a, b) => a + b, 0);
  return sum / count;
}
```
