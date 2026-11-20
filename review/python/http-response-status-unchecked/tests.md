# Tests: python/http-response-status-unchecked

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: the body is used without a status check (Examples: Flag)

covers: example:flag,required:no-check

```python path=app/api.py
def load(url):
    resp = requests.get(url, timeout=5)  # CANDIDATE
    return resp.json()
```

## Case: raise_for_status (Examples: OK)

covers: example:ok,do-not-flag:raise-for-status,required:raise

```python path=app/api.py
def load(url):
    resp = requests.get(url, timeout=5)
    resp.raise_for_status()
    return resp.json()
```

## Case: a status comparison (Do not flag)

covers: do-not-flag:status-compared

```python path=app/api.py
def load(url):
    resp = httpx.get(url)
    if resp.status_code != 200:
        return None
    return resp.text
```

## Case: httpx, a session and an awaited call

covers: detect:clients

```python path=app/api.py
async def a(url):
    r = await httpx.get(url)  # CANDIDATE
    return r.json()

def b(url):
    r = session.post(url, json={})  # CANDIDATE
    return r.content
```

## Case: a response that is not read

covers: detect:not-read

```python path=app/api.py
def ping(url):
    resp = requests.get(url, timeout=5)
    log.info("pinged")
```

## Case: a module that sets raise_for_status for its client

covers: do-not-flag:configured-client

```python path=app/client.py
hooks = {"response": lambda r, *a, **k: r.raise_for_status()}
def load(url):
    resp = requests.get(url, hooks=hooks, timeout=5)
    return resp.json()
```
