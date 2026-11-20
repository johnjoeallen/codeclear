# python/lambda-lifecycle-and-state: Unfinished work at return, warm-instance state and connection pressure

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
A Lambda handler returns while threads/tasks still run (frozen environment), keeps mutable global state across invocations of a warm instance, opens a DB connection per concurrent invocation, or mishandles retries for its invocation source (async invoke, SQS, stream).

## Why AI
Depends on the invocation model, concurrency settings and downstream limits.

## AI prompt
"At handler return, is any work (tasks, threads, buffered logs/metrics) unfinished? Does module-level mutable state leak between invocations? Under the configured concurrency, can connections exceed downstream limits? How does the invocation source retry on failure?" Supply handler, global setup, event source and concurrency/limits. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Init-time clients (the recommended pattern); global read-only caches; work awaited before return.

## Examples
Flag:
```python
def handler(e, c):
    asyncio.get_event_loop().create_task(send(e))    # frozen after return
    return {"statusCode": 200}
```
OK:
```python
def handler(e, c):
    asyncio.run(send(e)); return {"statusCode": 200}
```

## Evaluation cases
1. Above flag example -> flag.
2. `results = []` at module level appended each invocation -> flag.
3. boto3 client created at module level -> no finding.
4. New `psycopg2.connect` per invocation with reserved concurrency 1000 and a 100-connection DB shown -> flag.
