# java/reference-equality-on-values: == on strings or boxed numbers

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | java-review: Nulls and values |

## Intent
`==` on `String`, `Integer`, `Long` and other boxed types compares references, so results differ by value range and interning.

## Detect
A `==` or `!=` where both operands have static type `String` or a boxed numeric/`Boolean`/`Character` type.

## Do not flag
- Either operand is a `null` literal.
- Enum comparisons.
- Either operand is a primitive (unboxing makes it a value comparison).
- Comparison against a `static final` constant that is documented as an identity sentinel.

## Examples
Flag:
```java
if (status == "ACTIVE") { }
if (count1 == count2) { }   // both Integer
```
OK:
```java
if ("ACTIVE".equals(status)) { }
if (count1.equals(count2)) { }
```

## Implementation notes
Requires type resolution. Without it, only flag comparisons with a string literal.
Equivalent: Sonar `S4973`, ErrorProne `ReferenceEquality`.

## Required tests
String literal `==` (finding), two `Integer` variables (finding), `Integer == int` (no finding),
enum `==` (no finding), `== null` (no finding).
