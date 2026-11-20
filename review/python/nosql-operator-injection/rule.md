# python/nosql-operator-injection: User-supplied objects used as NoSQL filters

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Databases, ORM, and transactions; Security |

## Intent
Passing request-controlled dicts into a MongoDB-style query (`{"user": body["user"], "pwd": body["pwd"]}` with `{"$ne": null}`) lets the caller inject operators and bypass the filter. SQL dynamic identifiers/ORDER BY are handled by `sql-string-formatting`.

## Detect
Candidates: `find`, `find_one`, `update_*`, `delete_*`, `count_documents`, `aggregate` calls (pymongo/motor/mongoengine `objects(**x)`) whose filter contains a value or whole dict taken from `request.json`, `request.args`, `request.form`, `body`, `payload` without `str()`/schema validation.

## Do not flag
- Values coerced to scalars (`str(x)`), validated with a schema that forbids dicts, or constant filters.
- Filters built from server-side data only.

## Examples
Flag:
```python
db.users.find_one({"name": request.json["name"], "pwd": request.json["pwd"]})
```
OK:
```python
db.users.find_one({"name": str(body.name), "pwd_hash": hash_(body.pwd)})
```

## Implementation notes
Python `ast` taint-lite from request accessors to filter dict; anchor to the query call.

Detected in modules that mention `pymongo`, `motor` or `mongoengine`: `find`, `find_one`, `update_*`, `delete_*`, `replace_one`, `count_documents`, `aggregate`, `distinct`, `find_one_and_*` and mongoengine `objects(...)` with an argument that mentions `request`, or a variable assigned from it, and is not wrapped in `str(...)`. A function that mentions `validate`, `schema`, `Schema`, `pydantic` or `marshmallow` is treated as validating.

## AI prompt
Question: "Can the attacker supply an object (not a string) for this field, and does validation upstream prevent that?" Supply the handler and request model. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Dataflow candidates are deterministic; upstream schema validation decides reachability.
