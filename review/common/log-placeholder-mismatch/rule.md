# common/log-placeholder-mismatch: Log placeholders and arguments do not match

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | HIGH |
| Detection type | ast |
| Source | logging-review: What to check, Misleading messages (placeholders whose arguments are missing, extra or in the wrong order) |

## Intent
A message whose placeholder count differs from its argument count prints literal `{}` or drops values.

## Detect
Calls to SLF4J/Log4j2/Logback style loggers (`{}`), Python `logging` (`%s`/`%d` with positional args), and Go `slog` key/value pairs, where the number of placeholders in a string-literal format differs from the number of arguments. A trailing exception argument on `{}` loggers is not counted as an argument.

## Do not flag
- Non-literal format strings.
- Formats with numbered placeholders (`{0}`, `{0.00}`), which belong to `java.util.logging`, `MessageFormat` and Tinylog.
- A single `Object[]` argument, which SLF4J takes as the whole argument array (its elements are counted).
- `String.format`/`printf`, which are not logging calls.
- Argument order, which is not decidable deterministically (see `common/misleading-log-message`).
- Tests.

## Examples
Flag: `log.info("moved {} to {}", id);`
OK: `log.info("moved {} to {}", id, target);` and `log.error("failed {}", id, e);`

## Implementation notes
AST with a table of logger receivers and method names. Anchor to the call. Always `INFO`.

## Required tests
Missing argument (finding), extra argument (finding), trailing exception (no finding), escaped `\{}` (no finding), `%s` with two args and one placeholder (finding).

## Why deterministic
Counting placeholders and arguments in a literal format is a syntactic comparison needing no intent.
