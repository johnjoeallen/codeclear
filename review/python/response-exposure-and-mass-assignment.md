# python/response-exposure-and-mass-assignment: Serializer exposure, mass assignment and partial-update semantics

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Web frameworks and backend boundaries |

## Intent
Response models or serializers expose internal/sensitive fields; request data is bound directly to a model (`Model(**request.json)`, `fields = "__all__"`), letting callers set protected fields; partial updates confuse "absent" with null/default; business invariants are enforced on one entry point (HTTP) but not on jobs, scheduled tasks or persistence paths.

## Why AI
Needs the model's sensitive/protected fields, serializer config and the other entry points that write the same data.

## AI prompt
"Which fields can a caller read or write through this serializer/model binding? Does it include sensitive or protected fields (role, owner, password hash, price), treat omitted fields as null in PATCH, or skip an invariant enforced elsewhere?" Supply the model, serializer/schema, endpoint and other writers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Explicit allow-lists of fields; `read_only_fields`/`exclude` covering the sensitive ones; `model_dump(exclude_unset=True)` for PATCH.

## Examples
Flag:
```python
class UserSerializer(ModelSerializer):
    class Meta: model = User; fields = "__all__"      # includes is_staff, password
```
OK:
```python
fields = ["id", "name", "email"]
```

## Evaluation cases
1. Above flag example -> flag.
2. `User(**request.json)` with `is_admin` column -> flag.
3. PATCH handler: `obj.nickname = body.nickname` where absent means null -> flag.
4. Pydantic response model with explicit public fields -> no finding.
