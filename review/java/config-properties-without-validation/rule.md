# java/config-properties-without-validation: Configuration binding without validation

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONFIG |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Spring and distributed-service behavior |

## Intent
A `@ConfigurationProperties` or `@Value` binding whose invalid, missing or default value only fails at runtime (zero timeout, null URL, negative pool size) should be validated at startup.

## Detect
A `@ConfigurationProperties` class without `@Validated` and without constraint annotations on required fields, whose fields are used as timeouts, sizes, URLs or credentials; or a `@Value("${x}")` of a numeric/URL field with a default of `0`, empty string or `null`.

## Do not flag
- Classes with `@Validated` and constraints.
- Optional properties with safe, documented defaults.
- Properties not used on any failure-prone path.

## Examples
Flag:
```java
@ConfigurationProperties("payments") class PaymentProps { String baseUrl; int timeoutMs; }
```
OK: `@Validated` with `@NotBlank String baseUrl; @Min(1) int timeoutMs;`.

## Implementation notes
AST on annotations. Anchor to the class or field. Confirm profile/property files before treating a missing property as a defect.

Field-level: the field of a `@ConfigurationProperties` class without `@Validated`, whose name says timeout, url, size, port, secret and the like, with no constraint annotation and no default value; or an `@Value` field with a default of `0`, `null`, `false` or empty. Whether the property is used on a failure-prone path is left to the AI.

## Required tests
Unvalidated props class (candidate), `@Validated` + constraints (none), `@Value` default `0` for timeout (candidate).

## Why hybrid
Whether an invalid default causes a runtime failure depends on how the value is consumed and which profiles set it.

## AI prompt
"Does an unset or invalid value of this property cause a runtime failure in the shown profiles? Report only with the failing value and path." Evidence: the properties class, consuming code, application properties/profiles.
