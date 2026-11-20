# typescript/prototype-pollution: Untrusted object merged into another object

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Recursive merge or assignment by key path with attacker-controlled keys such as `__proto__` or `constructor.prototype` pollutes `Object.prototype` for the whole process.

## Detect
- `_.merge`, `_.defaultsDeep`, `_.set`, `$.extend(true, ...)`, `deepmerge`, `Object.assign` in a recursive helper, called with `req.body`/`req.query`/parsed JSON as source.
- Hand-written loops `target[key] = value` / `obj[a][b] = c` where keys come from input and `__proto__`/`constructor`/`prototype` are not rejected.

## Do not flag
- `Object.create(null)` targets, `Map`, keys filtered against forbidden names, patched library versions shown.
- Merges of server-controlled objects.

## Examples
Flag:
```ts
_.merge(config, req.body);
```
OK:
```ts
Object.assign({}, pick(req.body, ['name', 'email']));
```

## Implementation notes
Anchor to the merge/assignment.

Detected on `_.merge`, `mergeWith`, `defaultsDeep`, `set`, `setWith`, `$.extend`, `assignIn`, `deepmerge`, `merge` and similar calls with an argument derived from `req`, `request` or `body`, or from `JSON.parse`; and on an assignment `target[key] = value` where the key is derived from the request, or the function loops over input with `for...in`/`for...of` and mentions `req`, `body`, `input` or `payload`. A function that mentions `__proto__`, `constructor`, `hasOwnProperty`, `Object.create(null)`, `Map` or `Object.hasOwn` is skipped as a whole.

## Required tests
`_.merge(x, req.body)` (finding), allow-list `pick` (no finding), key loop without proto guard (finding), guarded loop (no finding).

## AI prompt
"Can attacker-chosen keys reach this merge/assignment, and is the library version or a guard preventing `__proto__`/`constructor` writes? Name the reachable operation affected." Evidence: call, source origin, dependency versions.

## Why hybrid
Sinks are listable; whether the source is attacker-controlled and the version patched needs context.
