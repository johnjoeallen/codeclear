# Tests: typescript/finally-overrides-result

A `// FLAG` marker ends each line that must produce a finding.

## Case: a return in finally (Examples: Flag)

covers: example:flag,required:return

```typescript path=src/work.ts
function f() {
  try { work(); } finally { return result; }  // FLAG
}
```

## Case: cleanup only (Examples: OK)

covers: example:ok,required:cleanup

```typescript path=src/work.ts
function f() {
  try { work(); } finally { cleanup(); }
}
```

## Case: throw, break and continue

covers: detect:statements

```typescript path=src/work.ts
function f(items) {
  try { work(); } finally { throw new Error('x'); }  // FLAG
  for (const i of items) {
    try { work(i); } finally { break; }  // FLAG
  }
  for (const i of items) {
    try { work(i); } finally { continue; }  // FLAG
  }
}
```

## Case: control flow fully contained in the finally (Do not flag)

covers: do-not-flag:contained,required:contained

```typescript path=src/work.ts
function f(items) {
  try { work(); } finally {
    for (const i of items) { if (i) { break; } continue; }
    const g = () => { return 1; };
    try { risky(); } catch (e) { log(e); }
    try { throw new Error('x'); } catch (e) { log(e); }
    g();
  }
}
```

## Case: cleanup that can throw, when the catch rethrows

covers: detect:cleanup

```typescript path=src/work.ts
async function f(conn) {
  try { await query(conn); } catch (e) { throw wrap(e); } finally {
    await conn.release();  // FLAG
  }
}
```

## Case: cleanup wrapped in its own try/catch, or a catch that does not rethrow (Do not flag)

covers: do-not-flag:wrapped-cleanup

```typescript path=src/work.ts
async function f(conn) {
  try { await query(conn); } catch (e) { throw wrap(e); } finally {
    try { await conn.release(); } catch (e2) { log(e2); }
  }
}
async function g(conn) {
  try { await query(conn); } catch (e) { log(e); } finally {
    await conn.release();
  }
}
```
