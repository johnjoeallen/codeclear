# Tests: python/config-defaults-and-import-side-effects

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: bool of an environment variable (Examples: Flag)

covers: example:flag,required:bool-getenv

```python path=app/settings.py
DEBUG = bool(os.getenv("DEBUG"))  # CANDIDATE
```

## Case: a comparison of the text (Examples: OK)

covers: example:ok,do-not-flag:real-default,required:comparison

```python path=app/settings.py
DEBUG = os.getenv("DEBUG", "").lower() in ("1", "true")
PORT = int(os.getenv("PORT", "8080"))
NAME = os.environ.get("NAME", "dev")
```

## Case: int without a default, and a production default

covers: detect:getenv

```python path=app/settings.py
PORT = int(os.getenv("PORT"))  # CANDIDATE
URL = os.environ.get("API_URL", "https://api.acme.io/v1")  # CANDIDATE
```

## Case: module-level side effects

covers: detect:import-side-effects,required:module-level

```python path=app/boot.py
engine = create_engine(URL)
conn = engine.connect()  # CANDIDATE
resp = requests.get("https://x")  # CANDIDATE
Thread(target=run).start()  # CANDIDATE
subprocess.run(["ls"])  # CANDIDATE
```

## Case: the same calls inside functions, classes and under __main__ (Do not flag)

covers: do-not-flag:inside-function-or-main

```python path=app/boot.py
def start():
    conn = engine.connect()
    requests.get("https://x")

class Worker:
    def run(self):
        subprocess.run(["ls"])

if __name__ == "__main__":
    app.run()
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_boot.py
DEBUG = bool(os.getenv("DEBUG"))
```
