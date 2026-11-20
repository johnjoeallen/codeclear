# java/bidirectional-relationship-inconsistent: One side of a bidirectional association updated

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Serialization, JPA and data contracts |

## Intent
When changed code mutates a bidirectional JPA association on only one side, the in-memory graph is inconsistent (and the inverse side is not persisted or is stale) until reload.

## Detect
In an entity with `@OneToMany(mappedBy=...)`/`@ManyToMany(mappedBy=...)`, a method that calls `children.add/remove(x)` without `x.setParent(this)`/`setParent(null)`, or a setter on the owning side that does not update the inverse collection.

## Do not flag
- Helper methods that update both sides.
- Unidirectional associations (no `mappedBy`).
- Immediate reload of the graph after persistence.

## Examples
Flag:
```java
void addItem(Item i) { items.add(i); }
```
OK:
```java
void addItem(Item i) { items.add(i); i.setOrder(this); }
```

## Implementation notes
AST on entity classes pairing `mappedBy` with the owner's field. Anchor to the one-sided mutation.

## Required tests
One-sided add (candidate), two-sided helper (none), unidirectional (none), remove without clearing parent (candidate).

## Why hybrid
Cross-class pairing is heuristic, and consequence depends on use of the graph before reload.

## AI prompt
"Is the inverse side read or persisted after this one-sided change in the shown code path? Report only with the wrong result." Evidence: both entity classes, the mutating method, subsequent reads.
