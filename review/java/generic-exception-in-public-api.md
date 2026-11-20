# java/generic-exception-in-public-api: Generic exception thrown from a public API

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | ERROR_HANDLING |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Exceptions |

## Intent
Public methods that declare or throw `Exception`, `RuntimeException` or `Throwable` leave callers unable to tell failures apart, so they cannot choose different recovery or status handling.

## Detect
- A `public` method of a public type with `throws Exception` or `throws Throwable`.
- `throw new RuntimeException(...)`, `new Exception(...)` or `new Throwable(...)` in such a method.

## Do not flag
- Overrides of interface methods that already declare `throws Exception` (`AutoCloseable.close`, `Callable.call`).
- `main`, test sources, and private/package-private methods.

## Examples
Flag:
```java
public Account load(String id) { throw new RuntimeException("not found"); }
```
OK:
```java
public Account load(String id) { throw new AccountNotFoundException(id); }
```

## Implementation notes
AST. Anchor to the `throws` clause or `throw` statement.

## Required tests
`throws Exception` (candidate), `throw new RuntimeException` in public method (candidate), `call()` override (none), private method (none).

## Why hybrid
Generic types are detectable; whether callers need distinct handling depends on how the API is used.

## AI prompt
"Do callers of this public method need to distinguish failure kinds (not found, conflict, invalid input, transient)? Report only if the diff shows callers or a status mapping that need it." Evidence: method, its callers or controller advice in the diff.
