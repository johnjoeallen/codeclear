# java/integer-overflow-risk: int arithmetic that can overflow

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Nulls and values |

## Intent
`int` counters, sizes and time arithmetic silently wrap past 2^31-1, producing negative or wrong values.

## Detect
- Multiplication of `int` operands involving a time unit constant or name (`* 1000`, `* 60`, `* 24`, `TimeUnit` factors) assigned to `int`/`long` without a `long` operand.
- `int` fields or locals named `count`, `total`, `size`, `bytes`, `millis`, `timeout` that are incremented in a loop or accumulate sizes.
- `(int)` casts of a `long` time or size value.

## Do not flag
- Multiplications where one operand is `long` or a `long` literal (`1000L`).
- Loop indices, bounded collection sizes and constants that fit in `int`.

## Examples
Flag:
```java
int timeoutMs = days * 24 * 60 * 60 * 1000;
```
OK:
```java
long timeoutMs = days * 24L * 60 * 60 * 1000;
```

## Implementation notes
AST with type resolution; constant-fold literals to see whether the product exceeds `Integer.MAX_VALUE`. Anchor to the expression.

Detected on the outermost `*` expression that contains a time or size factor (`1000`, `60`, `24`, `365`, `3600`, `1024`) and a name, with no `long` literal, `(long)` cast, `TimeUnit`, `Duration` or `multiplyExact`. A product of literals only is a constant the compiler can check. The `int` accumulators and `(int)` casts of the second and third bullets are not detected.

## Required tests
`int` product exceeding max (candidate), `1000L` (none), narrowing cast of `long` (candidate), loop index (none).

## Why hybrid
Types are deterministic; whether the real value range can reach the limit is a data question.

## AI prompt
"Can the operands of this `int` arithmetic reach a value that overflows in production?" Evidence: the expression, source of operands, realistic ranges, and any validation.
