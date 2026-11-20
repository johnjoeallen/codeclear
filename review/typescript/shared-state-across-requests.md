# typescript/shared-state-across-requests: Module-level mutable state leaks across requests or tenants

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Concurrency and request context |

## Intent
Request-specific data (user, tenant, headers, transaction) kept in module-level or singleton variables is overwritten by concurrent requests and can be served to the wrong caller. `AsyncLocalStorage` context can also be lost across callbacks, event emitters or workers.

## Why AI
Needs to know which values are request-scoped, how the module is instantiated, and whether context propagation is configured.

## AI prompt
"Is a request- or tenant-specific value written to state that outlives the request? Describe two overlapping requests where one reads the other's value. For ALS, name the callback or worker where context is lost and what then uses a missing or wrong context." Evidence: the module state, writers and readers, ALS setup.

## Do not flag
- Immutable config, caches keyed by tenant/user, or per-process counters.
- Request-scoped DI instances.

## Examples
Flag:
```ts
let currentUser: User; app.use((req, _r, next) => { currentUser = req.user; next(); });
```
OK:
```ts
app.use((req, _r, next) => als.run({ user: req.user }, next));
```

## Evaluation cases
1. Module variable set in middleware, read in handler: flag.
2. ALS store read inside `setTimeout` callback within `als.run`: ok.
3. Config object loaded once at startup: ok.
