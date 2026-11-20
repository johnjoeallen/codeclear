# java/transactional-on-private-method: @Transactional that Spring cannot intercept

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | HIGH for private/final, MEDIUM for self-invocation |
| Detection type | ast |
| Source | java-review: Spring; java-bug-hunter: Transactions and persistence boundaries |

## Intent
Spring's proxy-based transactions do not apply to `private` or `final` methods, or to a call from
another method in the same class. The method then runs without the expected transaction.

## Detect
1. `@Transactional` on a `private` method.
2. `@Transactional` on a `final` method of a class proxied by CGLIB.
3. An unqualified call (`foo()` or `this.foo()`) from a method in the same class to a method
   annotated `@Transactional` whose caller is not itself `@Transactional` with compatible propagation.

## Do not flag
- AspectJ weaving mode, when `mode = AdviceMode.ASPECTJ` is visible in the project configuration.
- Calls from a `@Transactional` method with the same or wider propagation (the transaction already exists).

## Examples
Flag:
```java
@Transactional
private void save(Order o) { }
```
OK:
```java
@Transactional
public void save(Order o) { }
```

## Implementation notes
AST on the class. Item 3 needs a same-class call graph, with `confidence: MEDIUM`. Anchor to the
annotation line (1, 2) or the call (3).

## Required tests
Private annotated method (finding), public annotated (no finding), self-call from non-transactional
method (finding), self-call from transactional method (no finding).
