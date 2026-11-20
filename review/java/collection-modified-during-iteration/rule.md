# java/collection-modified-during-iteration: Collection mutated while iterating

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-bug-hunter: Exceptions, resources and control flow |

## Intent
Adding or removing elements of a collection inside a for-each loop or `forEach` over the same collection throws `ConcurrentModificationException` or skips elements.

## Detect
Within an enhanced `for` over a collection expression `c`, or a lambda passed to `c.forEach(...)`/`c.stream().forEach(...)`, a call `c.add/remove/clear/put/addAll/removeAll(...)` on the same variable or field.

## Do not flag
- Mutation immediately followed by `break` or `return` out of the loop.
- Iteration over a copy (`new ArrayList<>(c)`, `List.copyOf(c)`) or a concurrent/copy-on-write collection.
- Iterator-based `it.remove()`.
- `put(k, v)` for the key being visited, which replaces a value without changing the map's structure.
- A variable declared as an interface but created as a concurrent or copy-on-write collection (`Map<K,V> m = new ConcurrentHashMap<>()`).

## Examples
Flag:
```java
for (Item i : items) { if (i.expired()) items.remove(i); }
```
OK: `items.removeIf(Item::expired);`

## Implementation notes
AST; compare the iterated expression to the receiver of the mutating call. Anchor to the mutating call. Equivalent: Sonar S2114.

## Required tests
Remove in for-each (finding), `removeIf` (none), iterator remove (none), iterate over copy (none), remove followed by `break` (none).

## Why deterministic
Same-variable iteration and mutation inside one loop body is a structural pattern.
