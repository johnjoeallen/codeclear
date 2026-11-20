# python/resource-leak-on-exit: Files, sockets, responses, subprocesses and locks not released on every path

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Resources, deployment, and shutdown; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Resources opened without a context manager or `try/finally` leak on exceptions and cancellation: files, sockets, HTTP responses/streams, `Popen` pipes, sessions, and manually acquired locks. Context managers that suppress cleanup failures can also hide leaks.

## Detect
Candidates:
- `x = open(...)`, `socket.socket(...)`, `requests.get(..., stream=True)`, `subprocess.Popen(...)`, `Session()`, `lock.acquire()` assigned outside a `with` and with a matching `close()`/`release()` not in `finally`;
- early `return`/`raise` between acquisition and release.

## Do not flag
- `with`/`async with`/`contextlib.closing`; resources returned to or owned by the caller or framework (documented); `ExitStack`.

## Examples
Flag:
```python
f = open(path); data = f.read(); f.close()     # read may raise
```
OK:
```python
with open(path) as f: data = f.read()
```

## Implementation notes
Python `ast`; equivalent Ruff `SIM115`, pylint `consider-using-with`. Anchor to the acquisition.

## AI prompt
Question: "Is the resource released on success, failure and cancellation, or owned by a framework/caller that closes it?" Supply the function and callers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Missing `with` is syntactic; ownership transfer is contextual.
