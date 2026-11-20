# python/http-call-without-timeout: HTTP client call with no timeout

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | python-review: Microservices and distributed side effects |

## Intent
`requests` has no default timeout. A stalled dependency blocks the caller indefinitely and can exhaust workers.

## Detect
A call to `requests.get|post|put|patch|delete|head|options|request(...)` or a `Session` method with no `timeout=` keyword. Also `urllib.request.urlopen(...)` without `timeout`.

## Do not flag
- Calls with `timeout=` (any value but `None`).
- `httpx` clients, which default to 5 seconds, unless `timeout=None` is passed.
- A `Session` with a documented timeout adapter visible in the same file.

## Examples
Flag:
```python
r = requests.get(url)
```
OK:
```python
r = requests.get(url, timeout=(3.05, 10))
```

## Implementation notes
Python `ast`. Equivalent: Ruff `S113`. `timeout=None` is also a finding.

## Why deterministic
A known HTTP client call lacking a `timeout` argument is a syntactic fact.

## Required tests
No timeout (finding), `timeout=5` (no finding), `timeout=None` (finding), `httpx.get` default (no finding).
