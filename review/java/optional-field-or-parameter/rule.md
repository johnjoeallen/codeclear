# java/optional-field-or-parameter: Optional used as a field or parameter

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Nulls and values |

## Intent
`Optional` fields and parameters are not serializable by default, break JSON/JPA mapping and invite null `Optional` arguments, violating an established API contract.

## Detect
- A non-static field whose type is `Optional<...>` in a class that is an entity, DTO, serializable class or `@ConfigurationProperties`.
- A method parameter of type `Optional<...>` in a public method.

## Do not flag
- `Optional` return types.
- Private methods and local variables.

## Examples
Flag:
```java
public class OrderDto { private Optional<String> note; }
```
OK:
```java
public class OrderDto { private String note; public Optional<String> note() { return Optional.ofNullable(note); } }
```

## Implementation notes
AST. Anchor to the field or parameter declaration.

## Left to the AI
Classes where the project explicitly serializes `Optional` (for example a Jackson `Jdk8Module`): the detector cannot see the configuration, so the AI confirms each candidate.

## Required tests
Optional field in DTO (candidate), Optional parameter (candidate), Optional return (none), local Optional (none).

## Why hybrid
The type is deterministic; whether it breaks serialization or a stated contract depends on the framework and conventions in use.

## AI prompt
"Does this Optional field or parameter break serialization, persistence mapping or an existing API contract visible in the context?" Evidence: the declaration, mapper/serializer configuration and sibling classes' conventions.
