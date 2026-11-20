# typescript/lock-ordering-and-ownership: Lock order, release and distributed-lock ownership

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Concurrency and request context; Caches and distributed locks; node-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Locks must be acquired in a consistent order, released on every exit, and (when distributed) released only by the owner. Leases that expire during work let a stale holder write after a new holder starts.

## Why AI
Deadlock, expiry and fencing depend on opposing code paths, lease durations and processing time that span files and runtime.

## AI prompt
"Identify (a) two paths acquiring locks in opposite order, (b) an exit path (throw, early return) that skips release, (c) a release that does not check ownership (e.g. `DEL` without a token compare), (d) work that can outlive the lease and write after a stale expiry. Report only with the concrete path and the invariant it breaks; require fencing only where a stale worker is shown to violate it." Evidence: all lock sites, TTL/lease values, processing duration, protected resource.

## Do not flag
- Locks released in `finally`, owner-checked scripts, or fencing tokens already present.
- Single-lock code with no opposing path.

## Examples
Flag:
```ts
await redis.set(key, '1', 'EX', 30, 'NX'); await work(); await redis.del(key);
```
OK:
```ts
const token = uuid(); await redis.set(key, token, 'EX', 30, 'NX'); try { await work(); } finally { await releaseIfOwner(key, token); }
```

## Evaluation cases
1. `del` after a possibly >30s job, no token: flag.
2. Release in `finally` with Lua compare-and-delete: ok.
3. A locks a then b, B locks b then a: flag.
