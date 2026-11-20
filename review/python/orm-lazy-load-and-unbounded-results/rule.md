# python/orm-lazy-load-and-unbounded-results: Detached lazy loads, N+1 queries and unbounded materialization

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions |

## Intent
Accessing a lazy relation or expired attribute after the session closed (`DetachedInstanceError`), issuing a query per item in a loop (N+1), loading an unbounded table into memory, leaving cursors open, or paginating without a stable order causes failures or material load.

## Why AI
Whether the relation is loaded (`joinedload`, `expire_on_commit`), the data volume and the scale impact need model definitions and expected sizes.

## AI prompt
"Does a changed access touch a lazy/expired attribute outside its session, execute a query per loop item, or `.all()`/`list()` a result set whose size is unbounded in production? Is the cursor closed and pagination ordered?" Supply the loop/handler, relationship loading config, and expected data sizes. Report only with a concrete trigger scenario and material impact; otherwise NO_FINDING.

## Do not flag
- `select_related`/`prefetch_related`/`selectinload` shown; small bounded tables; streaming with `yield_per`.

## Examples
Flag:
```python
for o in Order.objects.all():
    print(o.customer.name)           # query per row
```
OK:
```python
for o in Order.objects.select_related("customer").iterator():
    ...
```

## Evaluation cases
1. Above flag example on a table of millions of rows -> flag.
2. `session.close()` then `order.items` access with lazy="select" -> flag.
3. `.all()` on a 20-row lookup table -> no finding.
4. `OFFSET` paging with no `ORDER BY` -> flag.
