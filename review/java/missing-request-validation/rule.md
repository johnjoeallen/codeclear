# java/missing-request-validation: Request body not validated

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Spring; java-review: Backend boundaries and contracts; java-bug-hunter: Spring and distributed-service behavior |

## Intent
Invalid input reaches business logic when `@RequestBody` parameters lack `@Valid`/`@Validated`, or when nested request objects are not cascaded with `@Valid`.

## Detect
- A controller method parameter annotated `@RequestBody` (or a command object) without `@Valid`/`@Validated`, whose type has constraint annotations or is used for writes.
- A request DTO field of a nested DTO or collection type with no `@Valid`, while the nested type carries constraint annotations.

## Do not flag
- Parameters validated programmatically (`validator.validate(...)`) in the method.
- DTOs without constraints that are read-only filters.
- Project-wide validation through an aspect shown in context.

## Examples
Flag:
```java
@PostMapping("/orders") Order create(@RequestBody OrderRequest r) { ... }
```
OK:
```java
@PostMapping("/orders") Order create(@Valid @RequestBody OrderRequest r) { ... }
```

## Implementation notes
AST on `@RestController`/`@Controller` mappings. Anchor to the parameter.

Only a `@RequestBody` parameter of a `@PostMapping`, `@PutMapping`, `@PatchMapping` or `@RequestMapping` method is checked (a read-only `GET` filter is not a write). Nested DTO fields without `@Valid` are not detected, and neither is a validation aspect shown in context: those are left to the AI.

## Required tests
No `@Valid` (candidate), `@Valid` (none), nested DTO without `@Valid` (candidate), programmatic validate (none).

## Why hybrid
Whether invalid input is really harmful depends on downstream logic and global validation.

## AI prompt
"Can invalid input from this request reach business logic or persistence unchecked? Report only with a concrete invalid payload and its effect." Evidence: controller method, DTO definition, service use, validation config.
