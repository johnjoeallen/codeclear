# python/transaction-scope-and-release: Transaction boundary, commit/rollback and connection release

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Database invariants and transaction execution |

## Intent
A multi-step write is not covered by one transaction, commits too early/late, or the session/connection is not released on every exit path (autocommit, missing rollback, helper using another connection), breaking a business invariant.

## Why AI
Needs the named invariant, the ORM/driver's autocommit and nesting semantics, and every helper's connection ownership.

## AI prompt
"Name the invariant of this multi-step write. Trace every operation, including helpers and deferred QuerySet evaluation, to its connection/transaction. On failure at each step, is state consistent and is the session/connection rolled back and released?" Supply the function, helpers, ORM/driver and autocommit configuration. Report only with a concrete trigger scenario (failing step, resulting state, changed line); otherwise NO_FINDING.

## Do not flag
- Operations in one `atomic()`/`session.begin()` block; framework-owned session lifecycle shown to commit/rollback.

## Examples
Flag:
```python
debit(a, amt)           # commits in its own session
credit(b, amt)          # fails -> money lost
```
OK:
```python
with transaction.atomic():
    debit(a, amt); credit(b, amt)
```

## Evaluation cases
1. Above flag example with each helper opening its own session -> flag.
2. `conn = pool.get(); ...; conn.close()` with an exception path before close -> flag.
3. `with session.begin(): ...` containing all writes -> no finding.
4. Django view with `ATOMIC_REQUESTS=True` shown -> no finding.
