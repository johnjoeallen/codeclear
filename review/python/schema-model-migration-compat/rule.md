# python/schema-model-migration-compat: Model changes inconsistent with constraints, migrations or old/new versions

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Databases, ORM, and transactions |

## Intent
A model/column change (non-null without default, renamed/dropped column, new unique constraint) conflicts with visible constraints or migrations, or breaks the old application version running during a rolling deploy.

## Why AI
Needs migrations, existing data and deployment ordering; unseen migrations must not be inferred.

## AI prompt
"Compare the model change with the visible migration and constraints. Will the old and new code versions both work against the schema during rollout, and do existing rows satisfy the new constraint?" Supply model diff, migrations, and rollout notes. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Backward-compatible additive changes with defaults; changes whose migration is shown and handles old data.

## Examples
Flag:
```python
phone = models.CharField(max_length=20)        # NOT NULL, no default, existing rows, migration absent
```
OK:
```python
phone = models.CharField(max_length=20, null=True)
```

## Evaluation cases
1. Above flag example with migration shown adding NOT NULL without backfill -> flag.
2. Column renamed in one step while old pods still query the old name (rolling deploy shown) -> flag.
3. Added nullable column -> no finding.
4. Migration not in the MR and no evidence it is missing -> no finding.
