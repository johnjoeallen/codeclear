# typescript/nosql-operator-injection: Request object passed into a document query filter

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Databases, ORM, and transactions; node-review: Security |

## Intent
Passing `req.body`/`req.query` values directly into Mongo/Mongoose filters lets a client send `{"$ne": null}` or `$where` and bypass checks.

## Detect
`find/findOne/updateOne/deleteOne/aggregate/where` on a Mongoose/MongoDB collection with an argument that is `req.body`, `req.query`, `req.params`, a spread of one, or a property of one that is not coerced to string/number (`String(...)`, schema-validated).

## Do not flag
- Values validated by a schema, cast with `String()`/`Number()`, or `express-mongo-sanitize` in use.
- `$where` absent and operators from an allow-list.

## Examples
Flag:
```ts
User.findOne({ email: req.body.email, password: req.body.password });
```
OK:
```ts
User.findOne({ email: String(req.body.email) });
```

## Implementation notes
Anchor to the query call.

## Required tests
Direct body value (finding), `String()` cast (no finding), spread of `req.query` (finding), schema-validated DTO (no finding).

## AI prompt
"Can the client supply an object with `$` operators here, and does visible sanitization or validation prevent it?" Evidence: handler, middleware, schema.

## Why hybrid
The sink is local; sanitization may be global middleware.
