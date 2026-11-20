# python/swallowed-db-error-and-savepoints: Caught database error leaves partial commit or unusable transaction

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Database invariants and transaction execution |

## Intent
Catching a database error inside a transaction (Django `atomic`, SQLAlchemy session, psycopg) without a savepoint/rollback commits partial work or leaves the transaction in a failed state, so later statements raise or silently run outside intent. Also: ignored commit/rollback errors, row counts and version checks still yield a success response or published event.

## Why AI
Behaviour depends on ORM/backend semantics (PostgreSQL aborts the transaction, Django raises `TransactionManagementError`, SQLAlchemy needs rollback) and on the callers.

## AI prompt
"After the caught database exception (or ignored commit/rowcount/version failure), what is the state of the transaction and what does the caller see? Can partial work commit or a success/event be emitted?" Supply the try/except, enclosing transaction, ORM and backend. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Inner `atomic()` savepoint around the failing statement; explicit `session.rollback()`; errors re-raised.

## Examples
Flag:
```python
with transaction.atomic():
    try: Order.objects.create(...)       # IntegrityError
    except IntegrityError: pass
    Audit.objects.create(...)            # TransactionManagementError
```
OK:
```python
try:
    with transaction.atomic(): Order.objects.create(...)
except IntegrityError: ...
```

## Evaluation cases
1. Above flag example -> flag.
2. SQLAlchemy `except IntegrityError: log` then `session.commit()` without rollback -> flag.
3. `except IntegrityError: session.rollback(); raise Conflict` -> no finding.
4. `cursor.rowcount` ignored on `UPDATE ... WHERE version=%s` and 200 returned -> flag.
