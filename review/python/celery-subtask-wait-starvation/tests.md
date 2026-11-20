# Tests: python/celery-subtask-wait-starvation

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a task that waits for another task (Examples: Flag)

covers: example:flag,required:get

```python path=app/tasks.py
@app.task
def parent():
    result = child.delay(1)
    return result.get()  # CANDIDATE
```

## Case: a chain, and a get outside a task (Examples: OK)

covers: example:ok,do-not-flag:outside-task,required:chain

```python path=app/tasks.py
@app.task
def parent():
    return chain(child.s(1), other.s()).apply_async()

def view(request):
    return child.delay(1).get(timeout=5)
```

## Case: a direct wait, join and collect

covers: detect:waits

```python path=app/tasks.py
@shared_task
def a():
    return child.delay(1).get()  # CANDIDATE

@shared_task
def b():
    res = group(child.s(i) for i in range(3))()
    return res.join()  # CANDIDATE

@app.task
def c():
    r = child.apply_async(args=[1])
    return r.collect()  # CANDIDATE
```

## Case: disable_sync_subtasks=False

covers: detect:disable-check

```python path=app/tasks.py
@app.task
def a():
    return other.apply_async().get(disable_sync_subtasks=False)  # CANDIDATE
```

## Case: a get on something that is not a task result

covers: detect:other-get

```python path=app/tasks.py
@app.task
def a(config):
    return config.get("key")
```
