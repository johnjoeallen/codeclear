# typescript/unvalidated-runtime-payload: Untrusted JSON used without runtime validation

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | node-review: Values, serialization, and contracts; node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
TypeScript types and casts do not validate request bodies, queue messages or API responses. Absent fields, nulls, nested values, unknown enum values and coercion reach business logic or a sink. (The `as T` syntax itself is `unchecked-network-data`.)

## Why AI
Whether a field can be absent depends on the producer, the framework's validation pipeline (global pipes, middleware) and how the first use behaves, none of which is in one expression.

## AI prompt
"Follow this untrusted payload to its first use. Name a field that can be missing, null, wrongly typed, an unknown enum value or coerced, and state the incorrect outcome at that use. Report only with that concrete input." Evidence: handler, schema or DTO definitions, global validation config, the first use and any normalization/decoding before validation.

## Do not flag
- Payloads validated by zod, ajv, class-validator with enabled pipes, or similar before use.
- Fields where absence is handled at first use.

## Examples
Flag:
```ts
const { items } = req.body as Order;
items.forEach(price); // body without items throws
```
OK:
```ts
const { items } = OrderSchema.parse(req.body);
```

## Evaluation cases
1. `JSON.parse(msg.value).userId` used as DB key with no check: flag.
2. Body parsed with a zod schema first: ok.
3. NestJS DTO with global `ValidationPipe` shown: ok.
4. Cast only, but first use is `?.` with a safe default: ok.
