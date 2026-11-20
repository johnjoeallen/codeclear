# python/graceful-shutdown-draining: Shutdown does not stop intake, drain work or close resources within the termination budget

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Resources, deployment, and shutdown; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
On SIGTERM/shutdown the service keeps accepting work, drops in-flight requests/tasks, loses uncommitted acks/offsets, does not flush producers or buffered results, or cannot finish within the platform's termination grace period.

## Why AI
Needs the signal handling, lifecycle hooks, in-flight work duration and the deployment's termination budget.

## AI prompt
"On shutdown, does the code stop intake first, wait for in-flight handlers/tasks, commit/ack completed work, flush producers/logs and close clients, all within the visible termination grace period?" Supply shutdown hooks/signal handlers, worker loop, and grace-period config. Report only with a concrete trigger scenario (work lost or duplicated); otherwise NO_FINDING.

## Do not flag
- Framework-managed graceful shutdown shown to cover the path; idempotent, short work.

## Examples
Flag:
```python
signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))      # kills in-flight message before ack
```
OK:
```python
signal.signal(signal.SIGTERM, lambda *_: stop_event.set())   # loop stops polling, finishes current, commits, closes
```

## Evaluation cases
1. Above flag example in a consumer loop -> flag.
2. Producer never flushed on exit with buffered messages -> flag.
3. Uvicorn/Gunicorn with graceful timeout 30 s and handlers < 5 s -> no finding.
4. Shutdown awaits tasks with a 120 s timeout while the pod grace period is 30 s -> flag.
