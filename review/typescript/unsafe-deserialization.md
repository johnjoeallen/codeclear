# typescript/unsafe-deserialization: Unsafe deserialization of untrusted data

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Deserializers that can instantiate types or run code (`node-serialize`, `serialize-javascript` eval patterns, `js-yaml.load` with unsafe schema in old versions, `vm`) execute attacker payloads.

## Detect
`serialize.unserialize(x)`, `yaml.load(x)` with `DEFAULT_FULL_SCHEMA`/legacy `js-yaml` < 4, `eval(` of a JSON-like string, `new Function('return ' + x)`, `v8.deserialize(x)`/`msgpack` ext types from untrusted buffers, where `x` is non-literal and from a request, queue, cookie or file upload.

## Do not flag
- `JSON.parse`, `yaml.safeLoad`/`yaml.load` in js-yaml >= 4 (safe by default).
- Data from trusted internal storage.

## Examples
Flag:
```ts
const o = serialize.unserialize(req.cookies.session);
```
OK:
```ts
const o = JSON.parse(raw);
```

## Implementation notes
Anchor to the call. Check js-yaml version in `package.json`.

## Required tests
`unserialize` of request data (finding), `JSON.parse` (no finding), `yaml.load` v4 (no finding), `yaml.load` with FULL schema (finding).

## AI prompt
"Does attacker-controlled data reach this deserializer, and does this library/version instantiate objects or run code?" Evidence: call, data origin, dependency version.

## Why hybrid
Dangerous API names are listable; reachability and version safety need context.
