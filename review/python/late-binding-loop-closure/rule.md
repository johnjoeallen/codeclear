# python/late-binding-loop-closure: Late-bound loop variable captured by a callback

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Values, state, and control flow; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
A `lambda` or nested function defined in a loop (including `transaction.on_commit` callbacks and scheduled callbacks) reads the loop variable when it runs, after the loop ended, so every callback sees the last value.

## Detect
Inside a `for`/`while` body, a `lambda` or `def` that references the loop target (or a variable reassigned in the loop) as a free variable, and is passed to `on_commit`, `add_done_callback`, `call_soon`, `call_later`, `submit`, `Thread(target=)`, `create_task` wrapper, `.append`, or stored.

## Do not flag
- Value bound through a default argument (`lambda x=x: ...`) or `functools.partial`.
- Callback invoked synchronously within the same iteration.
- Variable never reassigned after the loop (single iteration proven).

## Examples
Flag:
```python
for o in orders:
    transaction.on_commit(lambda: publish(o.id))
```
OK:
```python
for o in orders:
    transaction.on_commit(lambda o=o: publish(o.id))
```

## Implementation notes
Python `ast` scope analysis. Equivalent: Ruff `B023`. Anchor to the closure.

## AI prompt
Question: "Does the callback run after the loop variable changes, so that it processes the wrong item?" Supply the loop and the API receiving the callback. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
Closure capture is syntactic, but whether execution is deferred depends on the receiving API.
