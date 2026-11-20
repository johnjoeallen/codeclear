# java/jpa-entity-equals-hashcode: Entity equals/hashCode that breaks collections or persistence

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Serialization, JPA and data contracts |

## Intent
An `equals`/`hashCode` based on a mutable or database-generated identifier changes while the entity sits in a `HashSet`/`HashMap`, so lookups and removals fail; Lombok `@Data` on entities does the same.

## Detect
In an `@Entity` class:
- `hashCode`/`equals` using `@GeneratedValue` ids or mutable fields (including Lombok `@EqualsAndHashCode`/`@Data`), or relationship fields.
- Entity instances added to a `Set`/`Map` key before `save` in the same method.

## Do not flag
- `hashCode` returning a constant (`getClass().hashCode()`) with `equals` on a non-null id.
- Equality on an immutable business key or assigned id.
- Entities never placed in hash-based collections.

## Examples
Flag:
```java
@Entity @Data class Item { @Id @GeneratedValue Long id; String name; }
```
OK: `equals` compares `id` when non-null; `hashCode` is `getClass().hashCode()`.

## Implementation notes
AST on annotations and overridden methods. Anchor to the annotation or method.

Only `@Data` and `@EqualsAndHashCode` (without `onlyExplicitlyIncluded = true`) on an `@Entity`, and a `hashCode` that reads a `@GeneratedValue` field or its getter, are detected. Mutable non-id fields, relationship fields and entities placed in a set before `save` are left to the AI.

## Required tests
`@Data` on entity (candidate), id-based `hashCode` on generated id (candidate), business-key equals (none), constant hashCode (none).

## Why hybrid
Harm requires the entity to be used in hash collections across a persist.

## AI prompt
"Is this entity placed in a hash-based collection across its identifier or field changes? Report only with the sequence that breaks lookups or duplicates entries." Evidence: entity, collection usage and the persist call.
