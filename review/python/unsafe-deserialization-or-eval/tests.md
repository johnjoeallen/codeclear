# Tests: python/unsafe-deserialization-or-eval

A `# FLAG` marker ends each line that must produce a finding.

## Case: yaml.load (Examples: Flag)

covers: example:flag,required:yaml-load

```python path=app/load.py
cfg = yaml.load(request.data)  # FLAG
```

## Case: yaml.safe_load (Examples: OK)

covers: example:ok,do-not-flag:safe-load,required:safe-load

```python path=app/load.py
cfg = yaml.safe_load(request.data)
a = yaml.load(data, Loader=yaml.SafeLoader)
b = yaml.load(data, yaml.CSafeLoader)
c = yaml.load(data, Loader=SafeLoader)
```

## Case: unsafe yaml loaders

covers: detect:yaml-loaders

```python path=app/load.py
a = yaml.load(data, Loader=yaml.FullLoader)  # FLAG
b = yaml.load(data, yaml.UnsafeLoader)  # FLAG
c = yaml.load_all(data)  # FLAG
```

## Case: pickle, marshal, shelve and dill

covers: detect:binary-formats,required:pickle

```python path=app/load.py
a = pickle.loads(blob)  # FLAG
b = pickle.load(fh)  # FLAG
c = cPickle.loads(blob)  # FLAG
d = marshal.loads(blob)  # FLAG
e = shelve.open(path)  # FLAG
f = dill.loads(blob)  # FLAG
g = json.loads(blob)
```

## Case: eval and exec of something that is not a literal

covers: detect:eval,required:eval

```python path=app/load.py
a = eval(user_input)  # FLAG
exec(f"x = {value}")  # FLAG
b = eval("1" + suffix)  # FLAG
```

## Case: eval and exec of string literals (Do not flag)

covers: do-not-flag:string-literal,required:literal

```python path=app/load.py
a = eval("1 + 2")
exec("import os")
b = eval("a" "b")
```

## Case: pickle of a file the program wrote itself is a known gap

covers: do-not-flag:self-written

```python path=app/load.py
with open(cache, "wb") as f:
    pickle.dump(data, f)
data = pickle.load(open(cache, "rb"))  # FLAG
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```python path=tests/test_load.py
a = pickle.loads(blob)
```
