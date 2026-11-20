# typescript/cache-key-isolation-ttl: Cache keys, TTL units, invalidation and stale values

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Caches and distributed locks |

## Intent
Cache keys must include every dimension that changes the value (tenant, user, locale, permissions). TTL units must match the client (`EX` seconds vs `PX` ms, node-cache seconds). Invalidation must occur at the right time for callers needing fresh data.

## Why AI
Which dimensions change the value and the required freshness are domain facts.

## AI prompt
"Does the cache key omit a dimension (tenant/user/role) so one caller can read another's value? Do the TTL units match the client API? Is the entry invalidated before or after the write so a stale value can be re-cached? Name the request sequence and wrong result." Evidence: key construction, set/get/del code, client docs, freshness requirement.

## Do not flag
- Public, caller-independent values, or caches where staleness is accepted.

## Examples
Flag:
```ts
await redis.set(`profile:${id}`, json, 'PX', 300); // meant 300 seconds: expires in 300 ms
```
OK: `await redis.set(\`profile:${tenantId}:${id}\`, json, 'EX', 300);`

## Evaluation cases
1. Key `report:${reportId}` for tenant-scoped data: flag.
2. TTL 300 with `PX` where the comment says 5 minutes: flag.
3. Invalidate after DB commit, read-through repopulation ordered correctly: ok.
