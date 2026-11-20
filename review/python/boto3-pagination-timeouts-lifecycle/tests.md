# Tests: python/boto3-pagination-timeouts-lifecycle

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: one page treated as complete (Examples: Flag)

covers: example:flag,required:one-page

```python path=app/files.py
import boto3
def names(bucket):
    resp = s3.list_objects_v2(Bucket=bucket)  # CANDIDATE
    return [o["Key"] for o in resp["Contents"]]
```

## Case: a paginator, a token loop, MaxKeys (Examples: OK)

covers: example:ok,do-not-flag:paginated,required:paginator

```python path=app/files.py
import boto3
def a(bucket):
    for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket):
        use(page)

def b(bucket):
    token = None
    while True:
        resp = s3.list_objects_v2(Bucket=bucket, ContinuationToken=token)
        token = resp.get("NextContinuationToken")

def c(bucket):
    return s3.list_objects_v2(Bucket=bucket, MaxKeys=1)
```

## Case: other list calls

covers: detect:list-calls

```python path=app/files.py
import boto3
def a():
    t = dynamodb.scan(TableName="t")  # CANDIDATE
    q = ec2.describe_instances()  # CANDIDATE
    r = iam.list_users()  # CANDIDATE
```

## Case: a client made per request or per iteration, and without a Config

covers: detect:client-lifecycle

```python path=app/files.py
import boto3
def handler(event):
    s3 = boto3.client("s3", config=CFG)  # CANDIDATE

def loop(buckets):
    for b in buckets:
        c = boto3.client("s3", config=CFG)  # CANDIDATE

def view(request):
    s3 = boto3.client("s3")  # CANDIDATE
```

## Case: a client at module level, in init code, with a Config (Do not flag)

covers: do-not-flag:module-level-client

```python path=app/clients.py
import boto3
s3 = boto3.client("s3")

def get_client():
    return boto3.client("s3", config=CFG)
```

## Case: a module that does not use boto3

covers: detect:other-library

```python path=app/db.py
def a():
    return session.query(User)
    return conn.scan()
```
