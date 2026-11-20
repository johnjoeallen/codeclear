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

## AI prompt
Question: "Is this literal a real credential (not a placeholder, hash, public identifier or test value)?" Supply the line, surrounding assignment and file path. Report only with a concrete exposed credential; otherwise NO_FINDING.

## Why hybrid
Patterns are deterministic; whether a literal is a real secret needs judgment.
