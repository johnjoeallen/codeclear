# Tests: typescript/prototype-pollution

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: merging the request body (Examples: Flag)

covers: example:flag,required:merge

```typescript path=src/settings.ts
export function update(req) {
  _.merge(settings, req.body);  // CANDIDATE
}
```

## Case: a merge of server-controlled objects (Examples: OK)

covers: example:ok,do-not-flag:server-objects,required:server-only

```typescript path=src/settings.ts
export function a() {
  _.merge(defaults, overrides);
}
```

## Case: other merge helpers and a variable

covers: detect:helpers

```typescript path=src/settings.ts
export function a(req) {
  const patch = req.query.patch;
  _.defaultsDeep(settings, patch);  // CANDIDATE
  deepmerge(settings, req.body);  // CANDIDATE
  _.set(settings, req.body.path, 1);  // CANDIDATE
  $.extend(true, settings, JSON.parse(req.body.json));  // CANDIDATE
}
```

## Case: a key from the input used as a property name

covers: detect:key-assignment

```typescript path=src/settings.ts
export function a(req) {
  const out = {};
  for (const key in req.body) {
    out[key] = req.body[key];  // CANDIDATE
  }
  return out;
}
```

## Case: forbidden names rejected, a Map, and a null-prototype target (Do not flag)

covers: do-not-flag:guarded,required:guarded

```typescript path=src/settings.ts
export function a(req) {
  const out = {};
  for (const key in req.body) {
    if (key === '__proto__' || key === 'constructor') continue;
    out[key] = req.body[key];
  }
  return out;
}
export function b(req) {
  const m = new Map();
  for (const key in req.body) { m.set(key, 1); }
}
```

## Case: a test file

covers: detect:test-file

```typescript path=src/settings.test.ts
export function update(req) {
  _.merge(settings, req.body);
}
```
