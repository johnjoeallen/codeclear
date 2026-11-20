# java/missing-endpoint-authorization: New endpoint without authorization

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Spring; java-review: Backend boundaries and contracts; java-bug-hunter: Spring and distributed-service behavior |

## Intent
A new entry point that should be restricted but carries no authorization check is reachable by any caller.

## Detect
A new or changed request-mapping method (`@GetMapping`, `@PostMapping`, ...) with no `@PreAuthorize`, `@Secured`, `@RolesAllowed`, no class-level equivalent, and no explicit principal/role check in the body.

## Do not flag
- Public endpoints by design (health, login, public docs) as named or documented in the diff.
- Endpoints covered by a URL-based security configuration shown in context.
- Controllers whose siblings are also unannotated because a gateway enforces access (when shown).

## Examples
Flag:
```java
@DeleteMapping("/users/{id}") void delete(@PathVariable long id) { users.delete(id); }
```
OK:
```java
@PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/users/{id}") void delete(@PathVariable long id) { ... }
```

## Implementation notes
AST for candidates. Anchor to the mapping annotation.

## Required tests
No annotation (candidate), method-level `@PreAuthorize` (none), class-level `@PreAuthorize` (none), `/health` (none).

## Why hybrid
Whether access should be restricted and where it is enforced is not in the method.

## AI prompt
"Should this endpoint be restricted, and is authorization enforced in the shown security configuration or gateway? Report only if the data or action is sensitive and no enforcement is visible." Evidence: the endpoint, class annotations, `SecurityFilterChain` config.
