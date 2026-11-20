# Tests: python/hardcoded-credentials

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a hardcoded password (Examples: Flag)

covers: example:flag,required:literal

```python path=app/db.py
password = "Pr0dPassw0rd!"  # CANDIDATE
```

## Case: from the environment, placeholders and labels (Examples: OK)

covers: example:ok,do-not-flag:placeholder-and-env,required:env

```python path=app/db.py
password = os.environ["DB_PASSWORD"]
api_key = os.getenv("API_KEY")
secret = "changeme"
token = "<your token>"
client_secret = "********"
token_count = "12345678901"
password_file = "/run/secrets/db"
EMPTY_PASSWORD = ""
users = {"password": "label-value-123"}
```

## Case: attributes, keyword arguments and default parameters

covers: detect:targets

```python path=app/db.py
self.api_key = "a1b2c3d4e5f6g7h8"  # CANDIDATE
connect(host, password="Sup3rSecret99")  # CANDIDATE
def login(user, client_secret="abcdef123456"):  # CANDIDATE
    pass
```

## Case: known formats and connection strings

covers: detect:formats

```python path=app/db.py
use("AKIAIOSFODNN7EXAMPL1")  # CANDIDATE
use("-----BEGIN RSA PRIVATE KEY-----")  # CANDIDATE
use("ghp_abcdefghijklmnopqrstuvwxyz0123456789")  # CANDIDATE
url = "postgresql://admin:s3cr3tpw@db.internal.local/app"  # CANDIDATE
```

## Case: an f-string and a short value (Do not flag)

covers: do-not-flag:not-literal

```python path=app/db.py
password = f"{prefix}-secret-value"
token = "abc"
```

## Case: a test file with fake values (Do not flag)

covers: do-not-flag:test-file

```python path=tests/test_db.py
password = "Pr0dPassw0rd!"
```
