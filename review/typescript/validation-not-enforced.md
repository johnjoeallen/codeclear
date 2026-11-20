# typescript/validation-not-enforced: Validation declared but not actually enabled or nested

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
Decorators or types (class-validator, Joi schemas, TS interfaces) validate nothing unless the pipe/middleware runs and nested objects are marked (`@ValidateNested`, `@Type`) and transforms enabled.

## Why AI
Enabling happens in bootstrap or global config in other files.

## AI prompt
"Is validation really executed for this input, including nested objects and arrays, with transformation/whitelist options? Name the invalid input that reaches business logic. Do not infer missing guards from a handler when global guards or pipes are unseen." Evidence: DTO, controller, `main.ts` / app bootstrap, validation options.

## Do not flag
- Inputs validated by a visible global `ValidationPipe`, schema middleware, or explicit `parse`.

## Examples
Flag: DTO with `@IsString() name` and a nested `address: AddressDto` without `@ValidateNested()`/`@Type`, so a malformed address passes.
OK: nested decorators present and global `ValidationPipe({ transform: true })` shown.

## Evaluation cases
1. Nested DTO property without `@ValidateNested`: flag.
2. Interface-typed body, no pipe/schema anywhere shown: flag.
3. Global pipe not in supplied files: ok (unknown).
