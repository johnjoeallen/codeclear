# python/dynamodb-conditional-and-batch-handling: Conditional writes, unprocessed items, decimals and read consistency

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
`batch_write_item`/`batch_get_item` can return `UnprocessedItems`/`UnprocessedKeys` that are ignored; conditional-check failures are treated as success or retried without the condition; floats passed to the resource API raise or lose precision (`Decimal` required); eventual-consistent reads used where read-after-write is needed.

## Detect
Candidates:
- `batch_write_item`/`batch_get_item` whose response is not inspected for `UnprocessedItems`/`UnprocessedKeys` (`batch_writer()` handles this);
- `except ClientError` around `put_item`/`update_item` with `ConditionExpression` that swallows `ConditionalCheckFailedException`;
- `float` values in `Item=`; `get_item`/`query` without `ConsistentRead=True` immediately after a write in the same function.

## Do not flag
- `table.batch_writer()`; handlers that distinguish conditional conflicts and unprocessed items; Decimal conversion.

## Examples
Flag:
```python
dynamodb.batch_write_item(RequestItems=req)      # response ignored
```
OK:
```python
resp = dynamodb.batch_write_item(RequestItems=req)
while resp.get("UnprocessedItems"): ...
```

## Implementation notes
Python `ast`; anchor to the call.

Two files. `rule.dsl` flags `batch_write_item`/`batch_get_item` in a function that never mentions `UnprocessedItems` or `UnprocessedKeys`; a float in `Item=`; and `get_item`/`query` without `ConsistentRead=True` in a function that also writes. `conditional.dsl` flags a `try` around a write with a `ConditionExpression` whose `except ClientError` (or `Exception`) neither re-raises nor mentions `ConditionalCheckFailed`. `batch_writer()` is another method, so it is never matched.

## AI prompt
Question: "Is a conflict/unprocessed item/stale read treated as success, or does a retry drop the original condition or repeat writes, causing an incorrect outcome?" Supply the call, handling code and the consistency requirement. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Missing response handling is syntactic; the required semantics (atomicity, consistency) are contextual.
