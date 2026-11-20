# typescript/in-memory-coordination-multi-instance: In-process lock, dedupe or counter used across instances

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Concurrency and request context |

## Intent
A `Map`, `Set`, mutex or counter in memory does not coordinate multiple processes, replicas, cluster workers or serverless instances, so a uniqueness, rate-limit or single-run guarantee fails.

## Why AI
Only deployment (replica count, cluster mode, serverless) shows if more than one instance exists.

## AI prompt
"Does correctness rely on this in-memory structure being global? Using the shown deployment (replicas, PM2/cluster, Lambda), describe two instances both passing the check. If no multi-instance deployment is shown, report nothing." Evidence: code, deployment manifests, intended invariant.

## Do not flag
- Per-instance optimisation caches.
- Single-instance deployments shown in config.

## Examples
Flag:
```ts
const seen = new Set<string>(); if (seen.has(id)) return; seen.add(id); // dedupe across 3 replicas
```
OK: dedupe through a unique DB constraint or Redis `SET NX`.

## Evaluation cases
1. In-memory rate limit, deployment with replicas: 3: flag.
2. Same code, single-instance deployment: ok.
3. Local memo of pure results: ok.
