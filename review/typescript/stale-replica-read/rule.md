# typescript/stale-replica-read: Read-after-write served from a replica

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Databases, ORM, and transactions; node-review: Microservices and distributed side effects |

## Intent
A flow that writes and then reads (or returns the read to the user) through a replica, cache or asynchronous projection can see old data.

## Why AI
Needs the replication topology, read-routing config and whether the flow requires read-after-write consistency.

## AI prompt
"Does this flow write and then read the same data through a path that may lag (read replica, `readPreference: secondary`, search index, projection)? Name the user-visible wrong result. Do not assume immediate consistency across async replicas, nor assume lag where routing is not shown." Evidence: code, read/write client config, consistency requirement.

## Do not flag
- Reads routed to the primary, or tolerant of staleness.

## Examples
Flag:
```ts
await primary.update(u); return await replica.findById(u.id);
```
OK: read from the primary or from the returned write result.

## Evaluation cases
1. Write then replica read returned to client: flag.
2. Write then read on the same connection: ok.
3. Dashboard read from replica: ok.
