# python/tenant-identifier-propagation: Tenant/user id not carried from authentication to queries, caches and downstream calls

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-bug-hunter: Trace the execution before judging it; python-review: Caches and distributed locks |

## Intent
A tenant or user identifier established at authentication is not applied to a database predicate, cache key, downstream request or response mapping, so one tenant can read or modify another's data (and shared cache keys cross tenants).

## Why AI
Needs the whole chain from auth to the final data access across files, and the privileged action reachable on bypass.

## AI prompt
"Follow the tenant/user id from authentication to every query predicate, cache key, downstream request and response. Where is it dropped or taken from client input, and what exact privileged action is reachable?" Supply handler, auth dependency, repository/cache code. Report only with a concrete trigger scenario (attacker, request, exposed data); otherwise NO_FINDING.

## Do not flag
- Global tables, shown tenant-scoping managers/row-level security, or caches keyed by tenant.

## Examples
Flag:
```python
cache.get(f"profile:{user_id}")           # user_id taken from the URL, no owner check, shared across tenants
```
OK:
```python
cache.get(f"t:{ctx.tenant_id}:profile:{ctx.user_id}")
```

## Evaluation cases
1. `Order.objects.get(pk=request.GET["id"])` for a tenant-owned model with no manager scoping -> flag.
2. Default manager shown to filter by the current tenant -> no finding.
3. Cache key lacks tenant while values are tenant data -> flag.
4. Downstream call forwards `X-Tenant` taken from the client header -> flag.
