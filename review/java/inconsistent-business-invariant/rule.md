# java/inconsistent-business-invariant: Business rule implemented differently across layers

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts |

## Intent
A business rule enforced in the controller, service and persistence paths with different conditions lets the same request succeed or fail depending on the entry path. Rules should have one owner.

## Why AI
Detecting this requires knowing the rule's intent (ticket, docs) and comparing semantics across several files; syntax alone cannot show two checks encode the same rule.

## AI prompt
"Identify the business rule changed in this diff. Find every other place in the supplied files that enforces the same rule. Report only if two paths produce different outcomes for a concrete input (give the input and both outcomes)." Evidence: the changed files, related controller/service/repository/validator code, ticket or MR description.

## Do not flag
- Defensive duplicate checks with identical semantics.
- Layer-appropriate checks (format validation in controller, uniqueness in persistence).

## Examples
Flag: controller accepts quantity >= 0, service rejects quantity < 1; `quantity = 0` yields a 500.
OK: controller validates shape; service owns the only quantity-range rule.

## Evaluation cases
1. Controller `@Min(0)`, service throws on `<1` - report with input `0`.
2. Controller `@NotNull`, DB `NOT NULL` - no report.
3. Rule changed in service only, batch importer bypasses service and uses old rule (both shown) - report.
4. Only one path visible - no report.
