# typescript/unencoded-url-input: URL built from user input without encoding

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | typescript-review: Security |

## Intent
Concatenating raw input into a path or query lets a value containing `/`, `?`, `&` or `#` change the URL structure.

## Detect
A template literal or `+` concatenation that forms an argument to `fetch`, `axios.*`, `new URL`, `location.href` or `window.open`, containing an interpolated identifier that is not wrapped in `encodeURIComponent` / `encodeURI` and is not a numeric-typed value, and whose name or origin is a parameter, `req.*`, `location.*`, `searchParams`, `props` or form input.

## Do not flag
- Values wrapped in `encodeURIComponent`, built with `URLSearchParams` or `url.searchParams.set`.
- Constants, enums and numbers.

## Examples
Flag:
```ts
fetch(`/api/users/${req.query.name}`);
```
OK:
```ts
fetch(`/api/users/${encodeURIComponent(name)}`);
```

## Implementation notes
Anchor to the interpolation. Do not apply to non-URL string building.

Detected on `fetch`, `axios.*`, `got`, `request`, `window.open` and `open` calls whose first argument is a template literal with `${}` or a `+` concatenation. A substitution is flagged when it is a parameter of the function, or mentions `req`, `location`, `searchParams`, `props`, `params` or `query`, and is not wrapped in `encodeURIComponent`/`encodeURI` and does not look numeric (`page`, `limit`, `offset`, `count`, `size`, `index`, ...). `new URL(...)` and `location.href = ...` are not matched.

## Required tests
Raw interpolation of input (finding), `encodeURIComponent` (no finding), `URLSearchParams` (no finding), numeric id (no finding).

## AI prompt
"Can the interpolated value contain URL metacharacters, and would that change the path, query or host the request reaches? Name the value that does it." Supply: the line, the value's origin, and any validation.

## Why hybrid
The sink is syntactic; whether the value is attacker-influenced and already validated is not.
