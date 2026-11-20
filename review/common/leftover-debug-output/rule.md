# common/leftover-debug-output: Debug print left in production code

| Field | Value |
|---|---|
| Applies to | any source file outside tests |
| Category | LOGGING |
| Severity | INFO |
| Confidence | HIGH |
| Detection type | regex |
| Source | logging-review: Leftover debugging output |

## Intent
Temporary `print` statements bypass log levels, formatting and routing, and may leak data.

## Detect
Per language, in non-test source:
- Java/Kotlin: `System.out.print*`, `System.err.print*`, `e.printStackTrace()`.
- JS/TS: `console.log`, `console.debug`, `debugger;`.
- Python: `print(`, `pprint(`, `breakpoint()`, `import pdb`/`pdb.set_trace()`.
- Go: `fmt.Print*` outside `main`.

## Do not flag
- Command-line tools and scripts whose purpose is console output (`main` methods, `bin/`, `scripts/`, `cli`).
- Test files, and `console.error` / `console.warn` in front-end error boundaries.

## Examples
Flag:
```python
print("user", user)
```
OK:
```python
log.debug("loaded user %s", user.id)
```

## Implementation notes
Always `INFO`, never costs score. If the printed value looks sensitive, also run `common/hardcoded-credential`.

## Required tests
Each listed call (finding), `main` method (no finding), test file (no finding).

## Why deterministic
A fixed per-language list of print and debugger calls in non-test, non-CLI files.
