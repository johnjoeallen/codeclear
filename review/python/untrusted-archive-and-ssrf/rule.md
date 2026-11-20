# python/untrusted-archive-and-ssrf: Unsafe archive extraction, symlink traversal and server-side request forgery

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Extracting untrusted archives without member-path checks allows writes outside the target (zip-slip, symlinks); fetching a user-supplied URL (or following its redirects) reaches internal services (SSRF). Deserialization and eval are in `unsafe-deserialization-or-eval`.

## Detect
Candidates:
- `tarfile.extractall`/`extract` (without `filter="data"`/`"tar"`), `zipfile.extractall`/`extract`, `shutil.unpack_archive` on data from uploads/requests;
- `requests`/`httpx`/`urllib`/`aiohttp` calls whose URL (or host) derives from request input, with `allow_redirects` defaulting/true and no host allow-list.

## Do not flag
- Per-member validation that resolves and checks containment, rejects links; `filter="data"`; allow-listed hosts with redirects disabled or revalidated.

## Examples
Flag:
```python
tarfile.open(upload).extractall(DEST)
requests.get(request.args["url"], timeout=5)
```
OK:
```python
tarfile.open(upload).extractall(DEST, filter="data")
```

## Implementation notes
Python `ast` taint-lite; anchor to the sink call.

## AI prompt
Question: "Is the archive/URL attacker-controlled, and do visible checks (member validation, host allow-list, redirect handling, network egress controls) prevent traversal or internal access?" Supply the handler and validation helpers. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Dangerous calls are mechanical; trust and mitigations are contextual.
