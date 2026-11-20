# python/lost-update-and-concurrent-upsert: Check-then-insert and read-modify-write races on persisted data

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions; python-bug-hunter: Database invariants and transaction execution |

## Intent
Reading a row then writing a derived value, or checking existence then inserting, loses updates or violates uniqueness when two requests interleave, unless isolation, row/version locks, unique constraints or conditional updates prevent it.

## Why AI
Needs the constraints, indexes, isolation level and locking visible elsewhere (models, migrations) plus a concrete interleaving.

## AI prompt
"Construct two interleaved transactions through this read-then-write or check-then-insert. Do `SELECT ... FOR UPDATE`, version columns, unique constraints, `F()` expressions, conditional updates or isolation level prevent the bad outcome?" Supply the code, model constraints and isolation settings. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- `UPDATE ... SET n = n + 1`, `F()` updates, `select_for_update`, `get_or_create` backed by a unique constraint with `IntegrityError` handled, `ON CONFLICT`.

## Examples
Flag:
```python
acct = Account.objects.get(pk=pk)
acct.balance += amt
acct.save()
```
OK:
```python
Account.objects.filter(pk=pk).update(balance=F("balance") + amt)
```

## Evaluation cases
1. Above flag example under default isolation -> flag.
2. `if not User.objects.filter(email=e).exists(): User.objects.create(email=e)` with no unique index -> flag.
3. Same with `unique=True` and `IntegrityError` handled -> no finding.
4. `with transaction.atomic(): a = Account.objects.select_for_update().get(pk=pk)` -> no finding.
