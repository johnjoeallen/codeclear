# java/double-checked-locking-without-volatile: Double-checked locking on a non-volatile field

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
Without `volatile`, another thread can observe a non-null but partially constructed object published by double-checked locking.

## Detect
Pattern `if (f == null) { synchronized (...) { if (f == null) { f = ...; } } }` where field `f` is a non-`volatile` (and non-`final`) reference field.

## Do not flag
- `volatile` fields.
- Holder-class idiom, enum singleton, `final` fields.
- Primitive fields (the pattern needs a null check, so only reference fields apply).

## Examples
Flag:
```java
private Config cfg;
Config get() { if (cfg == null) { synchronized (this) { if (cfg == null) cfg = load(); } } return cfg; }
```
OK: `private volatile Config cfg;`

## Implementation notes
AST pattern match on nested null checks with a `synchronized` block. Anchor to the field declaration or outer `if`. Equivalent: Sonar S2168.

## Required tests
Non-volatile DCL (finding), volatile DCL (none), single check in synchronized (none), holder idiom (none).

## Why deterministic
The two null checks, the synchronized block and the field modifiers are all syntactic.
