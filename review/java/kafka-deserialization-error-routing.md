# java/kafka-deserialization-error-routing: Deserialization errors and schema skew

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Kafka |

## Intent
A record that cannot be deserialised, or whose schema is incompatible, can fail the consumer in a loop at the same offset or be dropped silently.

## Why AI
It applies only when serializer, error-handler or schema registry configuration is visible and requires judging compatibility between producer and consumer schema versions.

## AI prompt
"Given the visible deserializer, `ErrorHandlingDeserializer`, error handler and schema/versions, state what happens to an undeserialisable or newer-schema record. Report only if it causes an endless retry at one offset or silent loss, with the configuration evidence. Skip if no serializer/schema context is supplied." Evidence: consumer config, `ErrorHandlingDeserializer`, schema or DTO diffs.

## Do not flag
- Records routed to a DLT by the error handler.
- Backward-compatible additive schema changes.

## Examples
Flag: `JsonDeserializer` configured directly, no `ErrorHandlingDeserializer`, DTO field type changed.
OK: `ErrorHandlingDeserializer` wrapping the delegate with a DLT recoverer.

## Evaluation cases
1. Raw `JsonDeserializer`, incompatible field type change - report.
2. `ErrorHandlingDeserializer` + DLT - no report.
3. Additive optional field - no report.
4. No config shown - no report.
