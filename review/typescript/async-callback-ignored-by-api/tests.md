# Tests: typescript/async-callback-ignored-by-api

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an async listener (Examples: Flag)

covers: example:flag,required:async-listener

```typescript path=src/events.ts
emitter.on('job', async (job) => {  // CANDIDATE
  await save(job);
});
```

## Case: a listener whose body is wrapped in try/catch (Examples: OK)

covers: example:ok,do-not-flag:try-catch,required:try-catch

```typescript path=src/events.ts
emitter.on('job', async (job) => {
  try {
    await save(job);
  } catch (e) {
    logger.error(e);
  }
});
```

## Case: timers

covers: detect:timers

```typescript path=src/timers.ts
setTimeout(async () => { await tick(); }, 100);  // CANDIDATE
setInterval(async function () { await poll(); }, 1000);  // CANDIDATE
setImmediate(async () => { await later(); });  // CANDIDATE
```

## Case: array methods that read the returned value

covers: detect:array-methods

```typescript path=src/arrays.ts
const a = items.filter(async (i) => await ok(i));  // CANDIDATE
const b = items.some(async (i) => await ok(i));  // CANDIDATE
items.sort(async (x, y) => compare(x, y));  // CANDIDATE
```

## Case: map feeding Promise.all, and sync callbacks (Do not flag)

covers: do-not-flag:map-promise-all,required:map

```typescript path=src/arrays.ts
const r = await Promise.all(items.map(async (i) => load(i)));
emitter.on('x', (e) => handle(e));
items.filter((i) => i.ok);
```

## Case: an async listener with no await

covers: detect:no-await

```typescript path=src/events.ts
emitter.on('job', async (job) => { save(job); });
```

## Case: handlers wrapped for Express, and test framework hooks (Do not flag)

covers: do-not-flag:express-wrapped,do-not-flag:test-hooks

```typescript path=src/wrapped.ts
router.get('/users', asyncHandler(async (req, res) => {
  res.json(await db.users());
}));
beforeEach(async () => { await reset(); });
it('works', async () => { await run(); });
```
