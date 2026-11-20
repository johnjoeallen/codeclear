# java/bigdecimal-misuse: BigDecimal from double or compared with equals

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Nulls and values; java-bug-hunter: Serialization, JPA and data contracts |

## Intent
`new BigDecimal(double)` carries binary floating-point error. `BigDecimal.equals` compares scale
as well as value, so `2.0` and `2.00` are not equal.

## Detect
- `new BigDecimal(x)` where `x` is a `double` or `float` literal or expression.
- `.equals(...)` called on an expression of type `BigDecimal`.

## Do not flag
- `new BigDecimal("0.1")`, `BigDecimal.valueOf(double)`, and `new BigDecimal(int/long)`.
- `.equals` inside a `equals()` implementation that intentionally compares scale.

## Examples
Flag:
```java
new BigDecimal(0.1);
price.equals(other);
```
OK:
```java
BigDecimal.valueOf(0.1);
price.compareTo(other) == 0;
```

## Implementation notes
Requires type resolution for the `equals` part. Constructor check is syntactic: a non-string
literal with a decimal point or an `f`/`d` suffix. Equivalent: SpotBugs `DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE`.

## Required tests
`new BigDecimal(0.1)` (finding), `new BigDecimal("0.1")` (no finding), `valueOf` (no finding),
`equals` on BigDecimal (finding), `compareTo` (no finding).
