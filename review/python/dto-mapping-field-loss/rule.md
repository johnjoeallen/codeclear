# python/dto-mapping-field-loss: DTO/model mapping drops fields, exposes sensitive data or skews versions

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow |

## Intent
Mapping between request/response DTOs, ORM models and message payloads silently drops a field, applies an incompatible default, copies a sensitive field, or disagrees with a visible producer/consumer version.

## Why AI
Needs the complete field lists of both sides and the intended contract; a new field only matters if a peer model/schema shows it should be mapped.

## AI prompt
"Compare source and target models field by field. Does the changed mapping lose a field that callers rely on, default it incompatibly, or copy a sensitive field to an outward-facing model?" Supply both model definitions, the mapper, and peer schemas if shown. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Fields intentionally excluded and documented; schema peers not shown.

## Examples
Flag:
```python
return UserOut(id=u.id, name=u.name)     # UserOut also requires email; u.password_hash added to dict(u)
```
OK:
```python
return UserOut.model_validate(u)         # UserOut declares exact public fields
```

## Evaluation cases
1. New column `phone` added to the model, mapper builds the DTO by hand and omits it while the API spec lists it -> flag.
2. `UserOut(**u.__dict__)` where `__dict__` holds `password_hash` and `UserOut` allows extras -> flag.
3. Mapper default `status="ACTIVE"` where the consumer treats missing as `PENDING` (shown) -> flag.
4. Field intentionally omitted with comment and spec agreement -> no finding.
