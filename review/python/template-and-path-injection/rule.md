# python/template-and-path-injection: Untrusted input in templates, file paths or dynamic code strings

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security; python-bug-hunter: Shared state, resources, and security boundaries |

## Intent
Request-controlled data reaching `render_template_string`/`Template(...)`/`Environment.from_string` (SSTI) or file-system paths (`open`, `send_file`, `os.path.join`) allows template execution or path traversal. SQL, shell and eval are covered by `sql-string-formatting`, `shell-injection-and-tls`, `unsafe-deserialization-or-eval`.

## Detect
Candidates: a value derived from `request.*`, handler params or message bodies passed as:
- the template source to `render_template_string`, `jinja2.Template`, `Environment.from_string`, `mako.Template`;
- a path component to `open`, `send_file`, `send_from_directory` (with absolute user path), `os.path.join`, `Path(...)`, `shutil.*`, `os.remove`, without `secure_filename`, a resolved-containment check or an allow-list.

## Do not flag
- Context variables passed to a constant template; `Path.resolve().is_relative_to(base)` checks; `secure_filename` result used under a fixed base.

## Examples
Flag:
```python
return render_template_string("Hello " + request.args["name"])
open(os.path.join(UPLOAD_DIR, request.args["f"]))
```
OK:
```python
return render_template_string("Hello {{ n }}", n=request.args["name"])
```

## Implementation notes
Python `ast` taint-lite; anchor to the sink.

Detected on `render_template_string`, `Template(...)`, `from_string(...)` with a source that is not a literal and is derived from `request` (or a variable assigned from it) or is a parameter of the enclosing function; and on `open`, `send_file`, `send_from_directory`, `os.path.join`, `Path`, `shutil.*`, `os.remove`, `os.listdir` with such an argument, unless the function mentions `secure_filename`, `is_relative_to`, `resolve`, `realpath`, `commonpath` or `allowed`.

## AI prompt
Question: "Is the value attacker-controlled at this sink, and does a visible validation/containment check prevent traversal or template injection?" Supply the handler and helpers. Report only with a concrete trigger scenario (payload, effect); otherwise NO_FINDING.

## Why hybrid
Sink reachability is mechanical; sanitizers and trust boundaries vary.
