# python/config-defaults-and-import-side-effects: Invalid environment defaults, import-time side effects and per-worker startup

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Resources, deployment, and shutdown |

## Intent
Environment parsing with unsafe defaults (`int(os.getenv("X"))` on `None`, `bool("false")` is `True`), or module-level code with side effects (network/DB connections, thread starts, migrations) that runs on every import and in every worker, fails startup or duplicates work.

## Detect
Candidates:
- `bool(os.getenv(...))`/`bool(os.environ.get(...))`; `int(os.getenv("X"))` with no default; `os.environ.get("X", "<prod-value>")` defaults;
- module-level (outside `def`/`class`/`if __name__`) calls to `requests.*`, `connect(`, `create_engine(...).connect()`, `Thread(...).start()`, `schedule`/`scheduler.start()`, `subprocess.*`, `app.run`.

## Do not flag
- Lazy engines (`create_engine` without connecting); module-level constants and pure setup; entry-point scripts.

## Examples
Flag:
```python
DEBUG = bool(os.getenv("DEBUG", "false"))     # "false" -> True
conn = psycopg2.connect(DSN)                  # at import
```
OK:
```python
DEBUG = os.getenv("DEBUG", "false").lower() == "true"
```

## Implementation notes
Python `ast`; anchor to the expression.

Detected on calls. `bool(os.getenv(...))`; `int`/`float` of `os.getenv("X")` with one argument; an `os.getenv`/`environ.get` default string that contains `prod`, `://`, `.com`, `.io`, `amazonaws` or `live`; and, at module level (outside any function, class or lambda and outside `if __name__ == "__main__"`), calls to `requests.*`, `connect`, `create_engine`, `Thread`, `scheduler.start`, `subprocess.*`, `app.run`, `urlopen` and `socket`.

## AI prompt
Question: "Does this default or import-time effect cause a demonstrated deployment/runtime failure (missing variable in prod, duplicate scheduler per worker)?" Supply the config source and worker model. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Patterns are deterministic; impact depends on deployment topology.
