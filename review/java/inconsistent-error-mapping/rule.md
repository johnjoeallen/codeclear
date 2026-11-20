# java/inconsistent-error-mapping: Entry point bypasses consistent error mapping

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-review: Backend boundaries and contracts; java-bug-hunter: Spring and distributed-service behavior |

## Intent
New controllers should map failures through the same mechanism as the rest of the service (`@ControllerAdvice`, problem-detail format). Bespoke try/catch or raw exceptions give clients inconsistent status codes and bodies.

## Why AI
Consistency is relative to conventions in other files (advice classes, existing controllers, API docs); there is no local syntactic signal.

## AI prompt
"Compare the error handling of this new or changed endpoint with the existing mapping mechanism. Report only if a specific failure (name it) produces a different status code or body than the same failure elsewhere, or leaks an internal exception." Evidence: the controller diff, the `@ControllerAdvice`/exception handlers, one or two sibling controllers.

## Do not flag
- Endpoints that delegate to the shared handler by throwing domain exceptions.
- Intentional protocol-specific responses (file download, streaming).

## Examples
Flag:
```java
@GetMapping("/{id}") ResponseEntity<?> get(@PathVariable long id) {
  try { return ok(svc.get(id)); } catch (Exception e) { return ResponseEntity.status(500).body(e.toString()); }
}
```
OK: the method throws `NotFoundException`, mapped by the existing `@ControllerAdvice`.

## Evaluation cases
1. Try/catch returning `500` with `e.toString()`, advice exists - report.
2. Throws `NotFoundException` handled by advice - no report.
3. Streaming download returning custom status - no report.
4. New controller, no advice visible in context - no report (state what would confirm).
