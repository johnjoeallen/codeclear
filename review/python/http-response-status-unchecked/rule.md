# python/http-response-status-unchecked: HTTP/gRPC response used without status or error classification

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | ERROR_HANDLING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Microservices and distributed side effects |

## Intent
A completed HTTP call does not mean success. Using `resp.json()`/`resp.text` without checking the status treats 4xx/5xx error bodies as data. Timeouts and connection reuse are covered by `http-call-without-timeout`.

## Detect
Candidates: a variable assigned from `requests.*`, `httpx.*` (sync/async) or `session.get/post` whose `.json()`, `.text`, `.content` or `.status_code` use is not preceded by `raise_for_status()` or a `status_code`/`ok`/`status` comparison on the same path. For `aiohttp`, `resp.json()` without `raise_for_status` or `resp.status` check. gRPC: call result used inside a broad `except` that ignores `RpcError`.

## Do not flag
- Clients configured with `raise_for_status=True`/event hooks; explicit status mapping.
- Fire-and-forget calls documented as best-effort.

## Examples
Flag:
```python
r = requests.get(url, timeout=3)
return r.json()
```
OK:
```python
r = requests.get(url, timeout=3); r.raise_for_status(); return r.json()
```

## Implementation notes
Python `ast` intraprocedural flow; anchor to the `.json()` use.

Detected on a variable assigned from `requests.<verb>`, `httpx.<verb>`, or a `<name>session`/`<name>client` call, when the function reads `.json`, `.text` or `.content` of it and mentions none of `raise_for_status`, `status_code`, `status` or `ok` anywhere; a file that mentions `raise_for_status` is treated as configuring it. `aiohttp` responses (`async with ... as resp`) and gRPC are not detected.

## AI prompt
Question: "Can the remote return a non-2xx response here, and does the caller treat the error payload as valid data or as success?" Supply the call, client configuration/hooks and consumer of the result. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The missing check is syntactic; client-level hooks and the harm of a bad payload need context.
