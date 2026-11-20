# java/external-payload-compatibility: External payload fields, enums and version skew

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Serialization, JPA and data contracts |

## Intent
Fields from external payloads may be null, absent or carry enum values the consumer does not know; producer and consumer can run different versions. Unchecked use fails at runtime.

## Why AI
It depends on the visible producer/consumer contract and mapper settings (`FAIL_ON_UNKNOWN_PROPERTIES`, `READ_UNKNOWN_ENUM_VALUES_AS_NULL`), which are not local to the field.

## AI prompt
"Trace each externally supplied field to its first use. Report only where a null/absent value, an unknown enum constant or a version-skewed payload throws or produces wrong data, citing the contract or mapper setting shown. Do not assume unseen producers." Evidence: DTO, deserialization config, schema/OpenAPI, first-use code.

## Do not flag
- Fields validated (`@NotNull`) before use.
- Enums with an `UNKNOWN` fallback or lenient mapper config.
- Contracts not visible.

## Examples
Flag: `Status.valueOf(payload.getStatus())` where the producer may add statuses.
OK: `Status.fromCode(payload.getStatus()).orElse(Status.UNKNOWN)`.

## Evaluation cases
1. `Enum.valueOf` on payload string, producer adds values per schema - report.
2. Field with `@NotNull` validated on entry - no report.
3. Strict mapper fails on unknown property while new producer version adds field (both shown) - report.
4. Contract unseen - no report.
