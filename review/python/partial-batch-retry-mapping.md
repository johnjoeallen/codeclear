# python/partial-batch-retry-mapping: Batch retry repeats already-completed non-idempotent work

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-bug-hunter: Database invariants and transaction execution; AWS and serverless failure windows |

## Intent
When a batch partially succeeds, retrying the whole batch repeats committed non-idempotent effects unless completed writes are mapped to the retry input.

## Why AI
Needs per-item completion state, the batch API's partial-failure semantics, and idempotency of each effect.

## AI prompt
"Map each item's completed work to the retry/ack/delete input. If item k fails after items 1..k-1 committed, does the retry repeat committed non-idempotent effects or drop uncommitted ones?" Supply the batch loop, retry/ack mechanism and effect semantics. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Idempotent per-item effects; per-item acknowledgement; atomic all-or-nothing batch.

## Examples
Flag:
```python
for r in records: charge(r)          # raises on 3rd; whole batch redelivered; 1 and 2 charged again
```
OK:
```python
failed = [r for r in records if not try_charge(r)]
return {"batchItemFailures": [{"itemIdentifier": r.id} for r in failed]}
```

## Evaluation cases
1. Above flag example -> flag.
2. Per-record idempotency key on `charge` -> no finding.
3. DB bulk insert in one transaction, retried whole -> no finding.
4. `bulk_create` ignoring conflicts then retry returning only failed ids but omitting some -> flag.
