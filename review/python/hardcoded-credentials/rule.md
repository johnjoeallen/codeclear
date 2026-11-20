# python/hardcoded-credentials: Secrets and credentials embedded in source

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security; AWS and serverless |

## Intent
Passwords, API keys, tokens, private keys or AWS access keys committed in source or defaults are exposed to everyone with repository access and cannot be rotated safely.

## Detect
Candidates: string literal assigned to or passed as a name matching `password|passwd|secret|token|api[_-]?key|private[_-]?key|aws_secret_access_key|client_secret` (case-insensitive) with length >= 8 and not obviously a placeholder; literals matching `AKIA[0-9A-Z]{16}`, `-----BEGIN .*PRIVATE KEY-----`, `xox[bp]-`, `ghp_[A-Za-z0-9]{36}`; connection strings `scheme://user:pass@host`; `os.environ.get("X", "<real-secret>")` defaults.

## Do not flag
- Placeholders (`changeme`, `<password>`, `***`), empty strings, env/secret-manager lookups, test fixtures with obviously fake values, names used as dictionary keys or labels.

## Examples
Flag:
```python
client = boto3.client("s3", aws_access_key_id="AKIAIOSFODNN7EXAMPLE", aws_secret_access_key="wJalr...")
```
OK:
```python
password = os.environ["DB_PASSWORD"]
```

## Implementation notes
Python `ast` for assignments/keywords plus regex on literals; entropy filter optional. Equivalent: Bandit `B105`-`B107`, gitleaks.

Detected on strings and on assignments, keyword arguments and default parameters. A secret-named target (including `self.password`) with a literal string of 8 or more characters, the known formats (AWS key, PEM header, Slack and GitHub tokens), and connection strings with `user:pass@`. Names that end in `count`, `length`, `type`, `url`, `file`, `path`, `name`, `id`, `header` and similar are labels, and dictionary keys are not looked at. Placeholder words skip a value.

## AI prompt
Question: "Is this literal a real credential (not a placeholder, hash, public identifier or test value)?" Supply the line, surrounding assignment and file path. Report only with a concrete exposed credential; otherwise NO_FINDING.

## Why hybrid
Patterns are deterministic; whether a literal is a real secret needs judgment.
