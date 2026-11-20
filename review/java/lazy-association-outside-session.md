# java/lazy-association-outside-session: Lazy association accessed after the persistence context closes

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | java-bug-hunter: Serialization, JPA and data contracts |

## Intent
Touching a lazy association after the transaction/session has ended (including during controller JSON serialization) throws `LazyInitializationException` or triggers hidden N+1 queries.

## Why AI
It needs the fetch type, whether a transaction or open-session-in-view spans the access, and the access path (service to controller to serializer), across files and config.

## AI prompt
"For each lazy association read in the shown path, state whether a session is open at that point (`@Transactional` scope, `spring.jpa.open-in-view`, fetch join, `@EntityGraph`). Report only an access outside any open session, naming the association and the access site, or an access in a loop that issues N+1 queries." Evidence: entity mappings, service method and its transaction annotation, controller/serializer code, JPA properties.

## Do not flag
- Associations fetched with `JOIN FETCH`/`@EntityGraph` or mapped as `EAGER`.
- Accesses inside a transactional method, or with open-in-view enabled (state this).
- DTO projections.

## Examples
Flag: non-transactional service returns `Order`; controller serializes `order.getItems()` (LAZY).
OK: service maps to `OrderDto` inside `@Transactional`.

## Evaluation cases
1. Entity with LAZY collection returned to Jackson, OSIV disabled - report.
2. Same, OSIV enabled by default and mentioned - no report.
3. `join fetch` in query - no report.
4. Loop calling `o.getCustomer().getName()` per order inside transaction - report (N+1).
