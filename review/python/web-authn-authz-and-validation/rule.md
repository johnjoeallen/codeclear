# python/web-authn-authz-and-validation: Missing authentication, resource/tenant authorization or nested validation on an endpoint

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Web frameworks and backend boundaries |

## Intent
A Django/DRF, Flask, FastAPI or Starlette endpoint performs a privileged action without the applicable authentication, per-resource or tenant authorization, validation of nested input, or consistent status/error mapping.

## Why AI
Protection may be in middleware, dependencies, decorators, permission classes or global settings in other files; absence in the handler alone proves nothing.

## AI prompt
"List every protection that applies to this route (middleware, dependencies, decorators, permission classes, global config). With them applied, can an unauthenticated or unauthorized caller reach the privileged action, or reach another user's/tenant's object?" Supply the route, router/app setup, permission/auth configuration and the object lookup. Report only with a concrete trigger scenario (caller, request, exposed action); otherwise NO_FINDING.

## Do not flag
- Intentionally public endpoints (health, login, webhooks with signature checks).
- Handlers protected by shown global/class-level permissions.

## Examples
Flag:
```python
@router.get("/invoices/{id}")
def get(id: int, user=Depends(current_user)):
    return Invoice.objects.get(pk=id)       # no owner/tenant filter
```
OK:
```python
return Invoice.objects.get(pk=id, tenant_id=user.tenant_id)
```

## Evaluation cases
1. Above flag example -> flag.
2. DRF view with `permission_classes = [IsAuthenticated, IsOwner]` -> no finding.
3. `@app.post("/admin/purge")` with no dependency and no global auth shown, router included publicly -> flag.
4. Handler lacks auth but router is included with `dependencies=[Depends(auth)]` -> no finding.
