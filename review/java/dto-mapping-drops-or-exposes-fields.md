# java/dto-mapping-drops-or-exposes-fields: DTO/entity mapping loses or leaks fields

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts |

## Intent
Mapping between DTO, domain and persistence models can drop required fields, change null/default semantics, or expose internal or sensitive fields (password hash, internal ids, audit data) in a response.

## Why AI
Whether a field is required, sensitive or has meaningful defaults is business knowledge; it needs both model classes and the contract.

## AI prompt
"Compare source and target models in this mapping. Report (a) a required field not copied, (b) null/default changed in a way that alters behaviour, or (c) an internal/sensitive field present in an externally returned type. Give the field and consequence." Evidence: mapper diff, both classes, API schema or consumer code, any `@JsonIgnore`/MapStruct config.

## Do not flag
- Fields intentionally omitted and documented, or ignored via `@Mapping(ignore = true)` with a reason.
- Internal-only DTOs not returned from endpoints.

## Examples
Flag: `return objectMapper.convertValue(userEntity, UserResponse.class)` where `UserEntity.passwordHash` is serialised.
OK: `UserResponse` has only `id`, `name`, `email` and a mapper copies them.

## Evaluation cases
1. Entity returned directly from controller containing `passwordHash` - report.
2. Mapper omits `currency`, which the target requires - report.
3. `@Mapping(target="auditLog", ignore=true)` - no report.
4. Mapper defaults null `status` to `ACTIVE` where old code left it null and callers distinguish - report.
