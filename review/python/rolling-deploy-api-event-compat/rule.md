# python/rolling-deploy-api-event-compat: API, event and stored-payload compatibility across versions

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Microservices and distributed side effects; python-bug-hunter: Distributed workflows and microservices |

## Intent
A changed API, event schema, serializer, cache format or stored payload breaks old consumers/producers during a rolling deploy or when older data is replayed; asynchronous projections are assumed immediately visible across replicas.

## Why AI
Needs the schemas/code of the visible peers and the rollout/replay facts; unseen consumers must not be invented.

## AI prompt
"Compare the old and new message/API/stored format with the visible peer code or schema. Does the old version of any peer fail or misread data produced by the new one (or vice versa, including replay of old messages)?" Supply the diff, peer schemas/consumers and rollout notes. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Additive optional fields with tolerant readers; versioned topics/endpoints.

## Examples
Flag:
```python
event = {"order_id": o.id, "total": str(o.total)}     # consumer (shown) does int(event["amount"])
```
OK:
```python
event = {"order_id": o.id, "amount": o.total, "total": str(o.total)}     # old field kept
```

## Evaluation cases
1. Field renamed in producer while a shown consumer reads the old name -> flag.
2. New required field added to a request model used by an old client shown in the repo -> flag.
3. Optional new field, consumer uses `.get` -> no finding.
4. No peer or schema shown -> no finding.
