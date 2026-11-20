# python/django-orm-evaluation-and-oncommit: Lazy QuerySets, on_commit callbacks and sync ORM in async contexts

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Asyncio, generators, and exception ownership; Database invariants and transaction execution |

## Intent
Lazy QuerySets evaluated outside the transaction (streaming responses, serializers, templates), `transaction.on_commit` hooks that do not run (no active atomic in tests, rolled back) or run with changed state, and sync ORM calls from `async def` (`SynchronousOnlyOperation`, event-loop blocking) break expectations.

## Why AI
Depends on Django version, `ATOMIC_REQUESTS`, adapters (`sync_to_async`, async ORM) and when the response iterates.

## AI prompt
"When is each QuerySet/queryset-derived value actually evaluated relative to the transaction and response? Is `on_commit` registered where it will run, and is sync ORM called from async code without `sync_to_async`/async API?" Supply the view/task, settings, Django version. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- `list(qs)` evaluated inside `atomic`; `await Model.objects.aget(...)`; `sync_to_async` wrappers.

## Examples
Flag:
```python
async def view(request):
    return JsonResponse(list(User.objects.all()))      # sync ORM in async view
```
OK:
```python
users = [u async for u in User.objects.all()]
```

## Evaluation cases
1. Above flag example -> flag.
2. `StreamingHttpResponse(qs.iterator())` created in an atomic view -> flag (runs after commit/close).
3. `with atomic(): obj.save(); on_commit(lambda: notify(obj.id))` -> no finding.
4. `on_commit` inside `except` branch after rollback of the outer atomic -> flag.
