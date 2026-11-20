# Tests: python/blocking-call-in-async

A `# FLAG` marker ends each line that must produce a finding.

## Case: time.sleep (Examples: Flag)

covers: example:flag,required:sleep

```python path=app/handler.py
async def handler():
    time.sleep(1)  # FLAG
```

## Case: asyncio.sleep (Examples: OK)

covers: example:ok,required:async-sleep

```python path=app/handler.py
async def handler():
    await asyncio.sleep(1)
```

## Case: the blocklist

covers: detect:blocklist

```python path=app/handler.py
async def handler():
    requests.get(u)  # FLAG
    urllib.request.urlopen(u)  # FLAG
    subprocess.run(["ls"])  # FLAG
    subprocess.check_output(["ls"])  # FLAG
    os.system("ls")  # FLAG
    psycopg2.connect(dsn)  # FLAG
    sqlite3.connect(path)  # FLAG
    open("big.bin").read()
```

## Case: a boto3 client created in the module

covers: detect:boto3

```python path=app/handler.py
s3 = boto3.client("s3")
async def handler():
    s3.get_object(Bucket="b", Key="k")  # FLAG
    other.get_object()
```

## Case: handed to a thread (Do not flag)

covers: do-not-flag:offloaded,required:to-thread

```python path=app/handler.py
async def handler():
    await asyncio.to_thread(time.sleep, 1)
    await loop.run_in_executor(None, requests.get, u)
    await loop.run_in_executor(None, lambda: requests.get(u))
    await anyio.to_thread.run_sync(time.sleep, 1)
```

## Case: nested plain functions and plain def endpoints (Do not flag)

covers: do-not-flag:nested-sync,do-not-flag:sync-endpoint,required:nested

```python path=app/handler.py
async def handler():
    def inner():
        requests.get(u)
    return inner

def endpoint():
    time.sleep(1)
```

## Case: an async function nested in a plain one

covers: detect:nearest

```python path=app/handler.py
def outer():
    async def inner():
        time.sleep(1)  # FLAG
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_handler.py
async def test_it():
    time.sleep(1)
```
