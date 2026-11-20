# Tests: python/validate-after-normalization

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a check and then an unquote (Examples: Flag)

covers: example:flag,required:check-then-unquote

```python path=app/files.py
def read(path):
    if ".." in path:
        abort(400)
    path = unquote(path)  # CANDIDATE
    return open(os.path.join(BASE, path)).read()
```

## Case: normalise first, then check (Examples: OK)

covers: example:ok,do-not-flag:normalised-first,required:normalise-first

```python path=app/files.py
def read(path):
    path = unquote(path)
    if ".." in path:
        abort(400)
    return open(os.path.join(BASE, path)).read()
```

## Case: startswith, lower and urljoin

covers: detect:forms

```python path=app/urls.py
def a(url):
    if not url.startswith("https://good.example"):
        abort(400)
    url = url.lower()  # CANDIDATE

def b(host):
    if host in ALLOWED_HOSTS:
        pass
    host = host.lower()  # CANDIDATE
```

## Case: a containment check on the final path (Do not flag)

covers: do-not-flag:containment

```python path=app/files.py
def read(path):
    if ".." in path:
        abort(400)
    path = os.path.normpath(path)
    full = (BASE / path).resolve()
    if not full.is_relative_to(BASE):
        abort(400)
    return open(full).read()
```

## Case: a change to another variable

covers: detect:other-variable

```python path=app/files.py
def read(path, name):
    if ".." in path:
        abort(400)
    name = name.lower()
```
