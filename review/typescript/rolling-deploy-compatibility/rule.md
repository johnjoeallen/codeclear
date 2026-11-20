# typescript/rolling-deploy-compatibility: Route, payload, event or cache-format change breaks old/new coexistence

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Microservices and distributed side effects; node-bug-hunter: Distributed workflows and microservices |

## Intent
During a rolling deploy or message replay, old and new versions talk to each other. Renamed or removed fields, changed enum values, and new cache or event formats can break the peer.

## Why AI
Needs the shown consumers/producers, stored messages and deploy strategy. Do not assume immediate consistency across async projections or replicas.

## AI prompt
"Compare old and new route/payload/event/cache shapes against the supplied consumers or stored messages. Name the consumer or replayed message that fails or misreads data. Report only if coexistence or replay is established." Evidence: diff of contract, consumer code, schema registry, deployment notes.

## Do not flag
- Additive optional fields, or changes with versioned routes/schemas shown.

## Examples
Flag: event field `userId` renamed to `customerId` while the shown consumer still reads `userId`.
OK: producer emits both during transition; consumer tolerant.

## Evaluation cases
1. Renamed field, consumer shown reading old name: flag.
2. New optional field: ok.
3. Cache value format changed, old pods read same keys: flag.
