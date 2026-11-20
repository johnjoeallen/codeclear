# python/validate-after-normalization: Untrusted data validated before decoding or normalization

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
A check on raw input (path, URL, filename) is bypassed because the value is later decoded or normalized (URL-decoding, Unicode normalization, `os.path.normpath`, `realpath`, case folding) before the dangerous operation.

## Detect
Candidates: within one function, a validation (`startswith`, `in`, `".." in`, allow-list membership, host comparison) on a variable that is then passed to `unquote`, `normalize`, `normpath`, `abspath`, `realpath`, `lower()`, or `urljoin` before reaching `open`, `send_file`, `requests.*`, `os.path.join` or `subprocess`.

## Do not flag
- Normalization performed first, validation on the result, and the validated value used.
- Containment checks using `Path.resolve().is_relative_to(base)` on the final path.

## Examples
Flag:
```python
if ".." in name: abort(400)
path = os.path.join(BASE, unquote(name))
```
OK:
```python
path = (Path(BASE) / unquote(name)).resolve()
if not path.is_relative_to(BASE): abort(400)
```

## Implementation notes
Python `ast` order-of-operations over a single variable's use-def chain; anchor to the validation.

## AI prompt
Question: "Does the later decoding/normalization allow input that passes the check to reach the final operation in a form the check would have rejected?" Supply the function, any sanitizer, and the sink. Report only with a concrete trigger scenario (payload, path reached); otherwise NO_FINDING.

## Why hybrid
Ordering is syntactic; exploitability depends on the sink and sanitizers visible.
