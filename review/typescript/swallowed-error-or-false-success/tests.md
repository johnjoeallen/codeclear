# Tests: typescript/swallowed-error-or-false-success

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an empty catch (Examples: Flag)

covers: example:flag,required:empty-catch

```typescript path=src/save.ts
export async function save(x) {
  try { await db.save(x); } catch (e) {}  // CANDIDATE
}
```

## Case: a catch that rethrows or wraps (Examples: OK)

covers: example:ok,do-not-flag:rethrow,required:rethrow

```typescript path=src/save.ts
export async function a(x) {
  try { await db.save(x); } catch (e) { throw new Error('save failed', { cause: e }); }
}
export async function b(x) {
  try { await db.save(x); } catch (e) { throw e; }
}
```

## Case: log and carry on, or return a success-shaped value

covers: detect:log-only

```typescript path=src/save.ts
export async function a(x) {
  try { await db.save(x); } catch (e) {  // CANDIDATE
    console.error('save failed', e);
    return true;
  }
}
export async function b() {
  try { return await db.all(); } catch (e) {  // CANDIDATE
    logger.error(e);
    return [];
  }
}
```

## Case: .catch and .then with a literal

covers: detect:promise-forms

```typescript path=src/save.ts
export function a() {
  return fetchAll().catch(() => []);  // CANDIDATE
}
export function b() {
  return fetchAll().then((r) => r, () => null);  // CANDIDATE
}
export function c() {
  return fetchAll().catch((e) => { throw e; });
}
```

## Case: a boundary that answers the caller, and best-effort cleanup (Do not flag)

covers: do-not-flag:boundary-catch,do-not-flag:best-effort-cleanup,do-not-flag:rethrow-wrap

```typescript path=src/save.ts
export async function a(req, res) {
  try { await db.save(req.body); } catch (e) {
    logger.error(e);
    res.status(500).json({ error: 'internal' });
  }
}
export async function b(conn) {
  try { await work(); } finally {
    try { await conn.close(); } catch (e) {}
  }
}
```
