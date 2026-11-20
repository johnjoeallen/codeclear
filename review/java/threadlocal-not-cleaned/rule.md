# java/threadlocal-not-cleaned: ThreadLocal not removed on pooled threads

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | java-bug-hunter: Concurrency and asynchronous work |

## Intent
Request/worker threads are reused; a `ThreadLocal` set but never removed leaks values into later requests and retains memory, especially when an exception skips cleanup.

## Detect
A `ThreadLocal` field with a `.set(...)` call in the class (or `MDC.put`, `SecurityContextHolder.setContext`, `LocaleContextHolder.set...`) where no `.remove()` (`MDC.remove/clear`, `clearContext`) on the same holder occurs in a `finally` block of the same method or in a filter/interceptor `afterCompletion`.

## Do not flag
- `ThreadLocal.withInitial` used only for non-leaking caches of fixed-size objects (e.g. `SimpleDateFormat`), `confidence: LOW`.
- Holders cleaned by a framework-managed callback in the same class (`afterCompletion`, `doFinally`).
- Test sources.

## Examples
Flag:
```java
CTX.set(user);
chain.doFilter(req, res);
```
OK:
```java
CTX.set(user);
try { chain.doFilter(req, res); } finally { CTX.remove(); }
```

## Implementation notes
AST on the enclosing method and class. Anchor to the `set` call.

## Required tests
`set` without remove (finding), remove in finally (none), remove outside finally (finding), `afterCompletion` removal (none).

## Why deterministic
The set/remove pairing and the `finally` placement are structural properties of the method.
