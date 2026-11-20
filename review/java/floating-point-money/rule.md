# java/floating-point-money: double or float used for money

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Nulls and values; java-bug-hunter: Serialization, JPA and data contracts |

## Intent
Binary floating point cannot represent decimal currency exactly; sums and rounding drift. Use `BigDecimal` or integer minor units.

## Detect
A field, parameter, local or return type of `double`, `float`, `Double` or `Float` whose name matches `(?i)(amount|price|cost|balance|total|fee|tax|salary|payment|money|currency)` and which participates in arithmetic (`+ - * /`) or rounding.

## Do not flag
- Names that denote ratios (`rate`, `percent`, `ratio`), counts, metrics or scores.
- Test sources.

## Examples
Flag:
```java
double total = price * quantity;
```
OK:
```java
BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
```

## Implementation notes
AST plus name heuristic. Anchor to the declaration. `new BigDecimal(double)` and `BigDecimal.equals` are covered by `bigdecimal-misuse`.

## Left to the AI
Display-only conversion at a UI boundary: the detector cannot tell, so the AI confirms each candidate.

## Required tests
`double amount` in arithmetic (candidate), `BigDecimal` (none), `double ratio` (none).

## Why hybrid
The name heuristic finds candidates; only the reviewer can tell whether the value is really money and whether rounding matters.

## AI prompt
"Does this floating-point value represent a monetary amount where rounding errors matter?" Evidence: declaration, arithmetic uses, and any persistence or API type the value flows to.
