# typescript/test-unreliable-assertions: Async tests that cannot fail or depend on time, randomness, network or order

| Field | Value |
|---|---|
| Applies to | `**/*.test.ts`, `**/*.spec.ts`, `**/*.test.js`, `**/*.spec.js` |
| Category | TESTING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Test evidence |

## Intent
A test that does not await its promise, has no assertion, asserts only against a mock it configured, or relies on wall-clock time, randomness, real network or test order gives false confidence or flakes.

## Detect
- `expect(...).resolves/rejects...` not awaited or returned; async test body calling an async function without `await`.
- A `test/it` body with no `expect`/`assert`/`should` call.
- `expect(true).toBe(true)`, `expect(x).toBeDefined()` as the only assertion.
- `setTimeout`-based sleeps, `Date.now()`, `Math.random()` or real `fetch` to external hosts without fakes.
- `it.only` / `describe.only` left in.

## Do not flag
- Tests whose assertions are in helper functions that are visibly awaited.
- Property tests with a seeded generator, fake timers.

## Examples
Flag:
```ts
it('saves', () => { expect(save(x)).resolves.toBe(1); });
```
OK:
```ts
it('saves', async () => { await expect(save(x)).resolves.toBe(1); });
```

## Implementation notes
Equivalent: `jest/valid-expect`, `jest/expect-expect`, `jest/no-focused-tests`. Anchor to the test.

## Required tests
Unawaited `resolves` (finding), no assertions (finding), awaited (no finding), fake timers (no finding), `.only` (finding).

## AI prompt
"Can this test still pass if the production behavior is broken? Name the defect it would miss." Evidence: test, code under test.

## Why hybrid
Structural defects are syntactic; whether the assertion is meaningful needs the code under test.
