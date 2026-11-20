# Tests: python/nosql-operator-injection

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a filter taken from the request body (Examples: Flag)

covers: example:flag,required:request-filter

```python path=app/users.py
import pymongo
def login(request):
    return db.users.find_one(request.json)  # CANDIDATE
```

## Case: values turned into strings (Examples: OK)

covers: example:ok,do-not-flag:str-values,required:str

```python path=app/users.py
import pymongo
def login(request):
    body = request.json
    return db.users.find_one({"name": str(body["name"]), "pw": str(body["pw"])})
```

## Case: through a variable, and the other methods

covers: detect:methods

```python path=app/users.py
import pymongo
def f(request):
    flt = request.json
    db.users.find(flt)  # CANDIDATE
    db.users.update_one(request.args, {"$set": {"x": 1}})  # CANDIDATE
    db.users.delete_many({"owner": request.form["owner"]})  # CANDIDATE
    db.users.count_documents(request.json)  # CANDIDATE
    User.objects(**request.json)  # CANDIDATE
```

## Case: a body validated against a schema (Do not flag)

covers: do-not-flag:schema

```python path=app/users.py
import pymongo
def f(request):
    body = LoginSchema().load(request.json)
    return db.users.find_one(body)
```

## Case: a filter that does not come from the request, and a module without a Mongo driver

covers: detect:other

```python path=app/users.py
import pymongo
def f(user_id):
    return db.users.find_one({"_id": user_id})
```

```python path=app/other.py
def f(request):
    return cache.find(request.json)
```
