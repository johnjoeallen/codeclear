# Tests: python/dynamodb-conditional-and-batch-handling

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: batch_write_item with the response ignored (Examples: Flag)

covers: example:flag,required:unprocessed

```python path=app/store.py
import boto3
def save(client, items):
    client.batch_write_item(RequestItems={"t": items})  # CANDIDATE
```

## Case: batch_writer, and unprocessed items handled (Examples: OK)

covers: example:ok,do-not-flag:batch-handled,required:batch-writer

```python path=app/store.py
import boto3
def a(table, items):
    with table.batch_writer() as w:
        for i in items:
            w.put_item(Item=i)

def b(client, items):
    resp = client.batch_write_item(RequestItems={"t": items})
    while resp.get("UnprocessedItems"):
        resp = client.batch_write_item(RequestItems=resp["UnprocessedItems"])
```

## Case: a float in the item

covers: detect:float

```python path=app/store.py
import boto3
def a(table):
    table.put_item(Item={"id": "1", "price": 19.99})  # CANDIDATE
    table.put_item(Item={"id": "2", "price": Decimal("19.99")})
```

## Case: a read after a write

covers: detect:consistent-read

```python path=app/store.py
import boto3
def a(table):
    table.put_item(Item={"id": "1"})
    return table.get_item(Key={"id": "1"})  # CANDIDATE

def b(table):
    table.put_item(Item={"id": "1"})
    return table.get_item(Key={"id": "1"}, ConsistentRead=True)

def c(table):
    return table.get_item(Key={"id": "1"})
```

## Case: a conditional write whose error is swallowed

covers: detect:conditional

```python path=app/store.py
import boto3
def a(table):
    try:  # CANDIDATE
        table.put_item(Item={"id": "1"}, ConditionExpression="attribute_not_exists(id)")
    except ClientError:
        log.warning("failed")

def b(table):
    try:
        table.put_item(Item={"id": "1"}, ConditionExpression="attribute_not_exists(id)")
    except ClientError as e:
        if e.response["Error"]["Code"] == "ConditionalCheckFailedException":
            return False
        raise
```
