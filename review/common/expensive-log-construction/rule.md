# common/expensive-log-construction: Costly log argument built when the level may be disabled

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | LOGGING |
| Severity | INFO |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | logging-review: What to check, Expensive or unsafe construction |

## Intent
Building large strings, serialising whole objects or calling expensive methods for a debug/trace log wastes CPU when the level is off.

## Detect
A debug/trace (or info in hot code) log call whose argument or message is eagerly computed by string concatenation of calls, `String.format`, `JSON.stringify`, `toJson`, `objectMapper.writeValueAsString`, `json.dumps`, f-string containing calls, or a method call that is not a plain getter, and which is not inside a level guard (`isDebugEnabled()`, `logger.isEnabledFor`) or passed lazily (supplier lambda, `%s` args).

## Do not flag
- Plain variables and trivial getters as arguments.
- Languages or libraries without a lazy option where the call is guarded.
- Error-level logs, which are normally enabled.
- Tests.

## Examples
Flag: `log.debug("payload " + mapper.writeValueAsString(order));`
OK: `if (log.isDebugEnabled()) { log.debug("payload {}", mapper.writeValueAsString(order)); }`

## Implementation notes
AST: log call at debug/trace level with a non-trivial eager argument and no enclosing level check. Anchor to the log call.

## Required tests
Eager serialisation (candidate), guarded (none), supplier lambda (none), plain getter (none), error level (none).

## AI prompt
"Is the argument expensive (large object serialisation, loop, I/O) or trivial? Is this code on a frequently executed path? Report only if the cost is non-trivial." Evidence: the call, argument expression types, enclosing loop or handler.

## Why hybrid
Eager evaluation is structural; whether the argument is actually expensive depends on the called method and data size.
