# python/unsafe-deserialization-or-eval: pickle, yaml.load, eval and exec

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | python-review: Security |

## Intent
These APIs execute code or construct arbitrary objects from their input. With untrusted input they allow remote code execution.

## Detect
- `pickle.load|loads`, `cPickle`, `marshal.load|loads`, `shelve.open`, `dill.load|loads`.
- `yaml.load(...)` without `Loader=SafeLoader` / `yaml.safe_load`, or with `FullLoader`/`UnsafeLoader`.
- `eval(x)` or `exec(x)` where `x` is not a string literal.

## Do not flag
- `yaml.safe_load`, `yaml.load(..., Loader=yaml.SafeLoader)`.
- `eval` / `exec` of string literals.
- Pickle of a file the program wrote itself in the same function: confidence `LOW`.

## Examples
Flag:
```python
cfg = yaml.load(request.data)
```
OK:
```python
cfg = yaml.safe_load(request.data)
```

## Implementation notes
Python `ast`. Equivalent: Bandit `B301`, `B506`, `B307`, `B102`; Ruff `S301`, `S307`, `S506`.
Whether the input is attacker-controlled is context; the AI reviewer can confirm.

## Why deterministic
Calls to `pickle.loads`, `yaml.load` without a safe loader, `eval` and `exec` are syntactic facts.

## Required tests
`yaml.load` (finding), `safe_load` (no finding), `pickle.loads` (finding), `eval(user)` (finding), `eval("1+1")` (no finding).
