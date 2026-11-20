# python/sqs-delete-visibility-partial-batch: SQS delete timing, visibility timeout, partial batch responses and poison routing

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
Messages deleted before processing completes are lost; processing longer than the visibility timeout causes duplicate delivery; a Lambda SQS batch that raises retries already-completed records (no `batchItemFailures`); poison messages lack a DLQ/`maxReceiveCount`; FIFO ordering/deduplication ids are wrong.

## Why AI
Needs queue configuration (visibility, redrive policy, FIFO, `ReportBatchItemFailures`) and per-record completion mapping.

## AI prompt
"Map each record's completed work to delete/partial-batch response. If record k fails after earlier ones completed, which are retried? Does processing time exceed the visible visibility timeout? How do poison messages exit? Are FIFO MessageGroupId/DeduplicationId correct?" Supply handler, event source mapping/queue config. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Idempotent handlers; `batchItemFailures` returned; DLQ shown.

## Examples
Flag:
```python
def handler(event, ctx):
    for r in event["Records"]: process(r)      # raises on 3rd; 1-2 reprocessed
```
OK:
```python
fails = []
for r in event["Records"]:
    try: process(r)
    except Exception: fails.append({"itemIdentifier": r["messageId"]})
return {"batchItemFailures": fails}
```

## Evaluation cases
1. Above flag example with non-idempotent `process` -> flag.
2. `delete_message` before `process` -> flag.
3. Handler returns `batchItemFailures` and `ReportBatchItemFailures` enabled -> no finding.
4. FIFO send with constant `MessageGroupId` for unrelated tenants where throughput matters (shown) -> flag.
