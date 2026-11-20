# Tests: typescript/unsafe-deserialization

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: unserialize of request data (Examples: Flag)

covers: example:flag,required:unserialize

```typescript path=src/load.ts
export function load(req) {
  return serialize.unserialize(req.body.data);  // CANDIDATE
}
```

## Case: JSON.parse and safeLoad (Examples: OK)

covers: example:ok,do-not-flag:safe-parsers,required:json

```typescript path=src/load.ts
export function a(req) {
  return JSON.parse(req.body.data);
}
export function b(req) {
  return yaml.safeLoad(req.body.text);
}
export function c(req) {
  return yaml.load(req.body.text);
}
```

## Case: eval, new Function and the full YAML schema

covers: detect:code-forms

```typescript path=src/load.ts
export function a(req) {
  const text = req.body.text;
  eval(text);  // CANDIDATE
  new Function('return ' + req.query.x);  // CANDIDATE
  yaml.load(text, { schema: yaml.DEFAULT_FULL_SCHEMA });  // CANDIDATE
  v8.deserialize(req.body.buffer);  // CANDIDATE
}
```

## Case: literals and data from trusted storage (Do not flag)

covers: do-not-flag:trusted-or-literal

```typescript path=src/load.ts
export function a() {
  eval('1 + 1');
  const cfg = readLocalConfig();
  return serialize.unserialize(cfg);
}
```

## Case: queue messages

covers: detect:message

```typescript path=src/consumer.ts
export function onMessage(message) {
  return v8.deserialize(message.value);  // CANDIDATE
}
```
