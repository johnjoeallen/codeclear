# Tests: python/late-binding-loop-closure

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a lambda handed to on_commit (Examples: Flag)

covers: example:flag,required:on-commit

```python path=app/hooks.py
for user in users:
    transaction.on_commit(lambda: notify(user))  # CANDIDATE
```

## Case: bound by a default argument (Examples: OK)

covers: example:ok,do-not-flag:default-binding,required:default-binding

```python path=app/hooks.py
for user in users:
    transaction.on_commit(lambda user=user: notify(user))
    transaction.on_commit(functools.partial(notify, user))
```

## Case: other deferred calls and stores

covers: detect:deferred,required:append

```python path=app/hooks.py
callbacks = []
for i, item in enumerate(items):
    callbacks.append(lambda: run(i))  # CANDIDATE
    future.add_done_callback(lambda f: done(item))  # CANDIDATE
    loop.call_later(1, lambda: tick(item))  # CANDIDATE
    t = Thread(target=lambda: work(item))  # CANDIDATE
    handlers[item] = lambda: use(item)  # CANDIDATE
```

## Case: a nested def

covers: detect:def

```python path=app/hooks.py
for user in users:
    def cb():  # CANDIDATE
        notify(user)
    transaction.on_commit(cb)
```

## Case: a callback run in the same iteration (Do not flag)

covers: do-not-flag:synchronous

```python path=app/hooks.py
for user in users:
    result = sorted(rows, key=lambda r: r[user])
    total = reduce(lambda a, b: a + b, [user.score])
    run_now(lambda: notify(user))
```

## Case: a function that does not use the loop variable, and one outside a loop (Do not flag)

covers: do-not-flag:no-loop-variable

```python path=app/hooks.py
for user in users:
    transaction.on_commit(lambda: refresh_cache())
transaction.on_commit(lambda: notify(user))
```
