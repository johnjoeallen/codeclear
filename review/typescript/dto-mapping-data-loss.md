# typescript/dto-mapping-data-loss: DTO mapping loses fields, leaks data or changes defaults

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Values, serialization, and contracts |

## Intent
Mapping between entity, DTO and API/event shapes can drop required fields, expose sensitive ones (password hash, internal ids) or silently change defaults.

## Why AI
Needs the target contract, consumers and which fields are required or sensitive.

## AI prompt
"Compare source and mapped shape. Name a required field dropped, a sensitive field exposed, or a default changed, and the consumer affected. Report schema/version incompatibility only against consumers or contracts shown." Evidence: both types, mapper, consumer/contract.

## Do not flag
- Intentional omission documented by the contract.
- Fields that no shown consumer reads.

## Examples
Flag:
```ts
return { ...user }; // includes passwordHash
```
OK:
```ts
return { id: user.id, name: user.name };
```

## Evaluation cases
1. Spread of an entity with `passwordHash` into the response: flag.
2. Mapper omits `currency` which the response type marks required: flag.
3. Explicit allow-list matching the schema: ok.
