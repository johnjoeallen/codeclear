# Tests: typescript/error-cause-lost

A `// FLAG` marker ends each line that must produce a finding.

## Case: a new error that ignores the caught one (Examples: Flag)

covers: example:flag,required:no-cause

```typescript path=src/db.ts
try { await db.query(q); } catch (e) { throw new Error('query failed'); }  // FLAG
```

## Case: cause: e (Examples: OK)

covers: example:ok,do-not-flag:uses-e,required:cause

```typescript path=src/db.ts
try { await db.query(q); } catch (e) { throw new Error('query failed', { cause: e }); }
try { await b(); } catch (err) { throw new Error(`failed: ${err.message}`); }
try { await c(); } catch (err) { throw new AppError('failed', err); }
```

## Case: rethrow of the error itself (Do not flag)

covers: do-not-flag:rethrow,required:rethrow

```typescript path=src/db.ts
try { await a(); } catch (e) { throw e; }
```

## Case: reject, and a catch without a binding

covers: detect:reject

```typescript path=src/db.ts
new Promise((resolve, reject) => {
  try { a(); } catch (e) { reject(new Error('x')); }  // FLAG
});
try { b(); } catch { throw new Error('y'); }  // FLAG
```

## Case: a security boundary that logs the original (Do not flag)

covers: do-not-flag:logged-boundary

```typescript path=src/db.ts
try { await a(); } catch (e) {
  logger.error('login failed', e);
  throw new Error('invalid credentials');
}
try { await b(); } catch (e) {
  console.error(e);
  throw new Error('invalid credentials');
}
```

## Case: an error thrown outside a catch, and in a function inside one

covers: detect:in-catch

```typescript path=src/db.ts
function f() { throw new Error('plain'); }
try { a(); } catch (e) {
  const g = () => { throw new Error('inner'); };
  use(e);
}
```

## Case: a test file

covers: detect:test-file

```typescript path=src/db.test.ts
try { a(); } catch (e) { throw new Error('x'); }
```
