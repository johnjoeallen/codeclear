# typescript/dynamodb-conditional-and-batch: Conditional updates, unprocessed items and read consistency

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless; node-bug-hunter: AWS and serverless failure windows |

## Intent
`BatchWriteItem`/`BatchGetItem` return `UnprocessedItems`/`UnprocessedKeys` that must be retried (only that subset, with conditions preserved). A conditional-write conflict is not a success. Eventually-consistent reads can miss a prior write.

## Why AI
Needs the invariant, retry code and whether the read must see the latest write.

## AI prompt
"Is `UnprocessedItems`/`UnprocessedKeys` handled and retried for only the failed subset? Are `ConditionalCheckFailedException` conflicts distinguished from success and from other errors, with conditions kept on retry? Does a read that needs the latest write use `ConsistentRead`? Name the lost or repeated write." Evidence: calls, retry wrapper, conditions, consistency requirement.

## Do not flag
- Batches of one with errors thrown; reads tolerant of staleness.

## Examples
Flag:
```ts
await ddb.send(new BatchWriteCommand({ RequestItems })); // UnprocessedItems ignored
```
OK: loop re-sending `res.UnprocessedItems` with backoff.

## Evaluation cases
1. Batch write, response ignored: flag.
2. Retry resends the whole batch including successes (non-idempotent increments): flag.
3. `GetItem` after `PutItem` needing latest, default consistency: flag.
4. Conflict exception translated to 409: ok.
