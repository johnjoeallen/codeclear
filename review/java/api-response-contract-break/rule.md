# java/api-response-contract-break: Changed API response breaks an existing consumer

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts |

## Intent
Removing, renaming, retyping or changing the semantics/status of a response field breaks consumers that rely on the previous contract.

## Why AI
Breakage depends on consumers and published schemas that are only sometimes in the supplied context; the reviewer must not assume unseen consumers.

## AI prompt
"Compare the old and new response shape (fields, types, nullability, status codes, enum values). Report only if a consumer or contract (OpenAPI, client code, contract tests) shown in the context depends on what changed; name it." Evidence: the diff, OpenAPI/Pact files, consuming client code.

## Do not flag
- Additive optional fields.
- Changes to endpoints marked internal/unreleased in the diff.
- Breakage claims with no visible consumer.

## Examples
Flag: field `total` renamed to `totalAmount`, while `openapi.yaml` and a shown Feign client still use `total`.
OK: new optional `currency` field added.

## Evaluation cases
1. Rename with visible client using old name - report.
2. Added optional field - no report.
3. Status 200 changed to 201 and contract test asserts 200 - report.
4. Rename, no consumer shown - no report.
