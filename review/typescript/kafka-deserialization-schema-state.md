# typescript/kafka-deserialization-schema-state: Deserialization, schema compatibility and partition state cleanup

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Kafka |

## Intent
Consumers must handle undecodable or schema-changed records without crashing the partition, and must clear partition-specific in-memory state on revocation.

## Why AI
Requires the schema/registry, serializer config and consumer state model.

## AI prompt
"What happens when `JSON.parse`/Avro decode fails or a field is missing/new? Is per-partition state (buffers, caches, offsets) cleared on rebalance/revocation? Report only with a concrete record or rebalance scenario." Evidence: deserializer, schema definitions, rebalance handlers, state containers.

## Do not flag
- Decoding failures routed to DLQ (see `kafka-poison-message-handling`); stateless handlers.

## Examples
Flag:
```ts
const evt = JSON.parse(message.value!.toString()); use(evt.user.id); // throws on schema change
```
OK: validated decode with explicit failure routing.

## Evaluation cases
1. Per-partition buffer never cleared on rebalance: flag.
2. Strict decode with failure routing: ok.
3. New required field read without default while old events replay: flag.
