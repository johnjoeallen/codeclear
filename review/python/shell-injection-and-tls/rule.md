# python/shell-injection-and-tls: shell=True and disabled TLS verification

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Security |

## Intent
`shell=True` with built strings enables command injection. `verify=False` removes certificate validation and allows interception.

## Detect
- `subprocess.*(..., shell=True)` or `os.system(...)` / `os.popen(...)` where the command is not a string literal.
- Any call with keyword `verify=False`, `ssl._create_unverified_context()`, `ssl.CERT_NONE`, or `check_hostname = False`.

## Do not flag
- `shell=True` with a constant string and no interpolation.
- `verify=False` in test files.

## Examples
Flag:
```python
subprocess.run(f"convert {name}", shell=True)
requests.get(url, verify=False, timeout=5)
```
OK:
```python
subprocess.run(["convert", name], check=True)
```

## Implementation notes
Python `ast`. Equivalent: Bandit `B602`, `B605`, `B501`; Ruff `S602`, `S501`.

## Why deterministic
`shell=True` with a non-constant command and `verify=False`/unverified SSL contexts are syntactic facts.

## Required tests
f-string with `shell=True` (finding), list arguments (no finding), `verify=False` (finding), test path (no finding).
