# typescript/null-vs-missing-patch-semantics: Missing field and explicit null treated alike

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Values, serialization, and contracts |

## Intent
Update or PATCH handlers must tell "field omitted" (keep) from "field null" (clear). Merging with `??`, `||` or `if (body.x)` conflates them and either wipes or keeps data wrongly.

## Why AI
The intended contract (clear vs keep) lives in the API spec or ticket, not the code.

## AI prompt
"Does this update path need to distinguish an omitted field from explicit null or an empty value? Name the request that produces the wrong stored state." Evidence: handler, API contract/OpenAPI or ticket, DTO, how the ORM treats `undefined` vs `null`.

## Do not flag
- Full-replace (PUT) semantics.
- Fields that are not nullable.

## Examples
Flag:
```ts
user.nickname = body.nickname || user.nickname; // cannot clear nickname
```
OK:
```ts
if ('nickname' in body) user.nickname = body.nickname;
```

## Evaluation cases
1. PATCH with `||` fallback and a nullable column: flag.
2. `'x' in body` check: ok.
3. PUT replacing all fields: ok.
