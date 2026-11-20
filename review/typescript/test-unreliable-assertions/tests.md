# Tests: typescript/test-unreliable-assertions

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a test with no assertion (Examples: Flag)

covers: example:flag,required:no-assertion

```typescript path=src/cart.test.ts
it('adds an item', () => {  // CANDIDATE
  cart.add(item);
});
```

## Case: a test that asserts (Examples: OK)

covers: example:ok,do-not-flag:asserting-test,required:asserts

```typescript path=src/cart.test.ts
it('adds an item', () => {
  cart.add(item);
  expect(cart.size()).toBe(1);
});
```

## Case: .only left in

covers: detect:only

```typescript path=src/only.test.ts
describe.only('cart', () => {  // CANDIDATE
  it.only('adds', () => { expect(1).toBe(1); });  // CANDIDATE
});
fit('x', () => { expect(1).toBe(1); });  // CANDIDATE
```

## Case: assertions that cannot fail

covers: detect:weak

```typescript path=src/weak.test.ts
it('a', () => {
  expect(true).toBe(true);  // CANDIDATE
});
it('b', () => {  // CANDIDATE
  expect(result).toBeDefined();
});
```

## Case: a promise assertion that is not awaited

covers: detect:resolves

```typescript path=src/promise.test.ts
it('a', async () => {
  expect(load()).resolves.toBe(1);  // CANDIDATE
  await expect(load()).resolves.toBe(1);
});
```

## Case: the clock, chance and the network

covers: detect:nondeterminism

```typescript path=src/clock.test.ts
it('a', () => {
  const now = Date.now();  // CANDIDATE
  const r = Math.random();  // CANDIDATE
  expect(now).toBeGreaterThan(r);
});
```

## Case: fake timers and mocked network (Do not flag)

covers: do-not-flag:fakes

```typescript path=src/fake.test.ts
jest.useFakeTimers();
it('a', () => {
  const now = Date.now();
  expect(now).toBe(0);
});
```
