# python/periodic-job-overlap: Scheduled jobs overlapping, duplicated across schedulers or interrupted mid-work

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CONCURRENCY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: RabbitMQ, Celery, and scheduled jobs; python-bug-hunter: Kafka, RabbitMQ, and Celery |

## Intent
A periodic job (Celery beat, APScheduler, cron) runs again while the previous run is still active, runs on multiple scheduler instances, or leaves partial work on shutdown/time limit, violating an invariant such as "process each record once".

## Why AI
Needs the schedule, the run duration, scheduler topology and coordination (locks, DB claim) shown elsewhere.

## AI prompt
"Can two runs of this job execute concurrently (long run > interval, several beat/scheduler instances/replicas)? If so, which shown invariant is violated, and what happens to partial work on shutdown or time limit?" Supply the job, schedule configuration, deployment of the scheduler and any lock/claim logic. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Jobs that are idempotent or claim work with `SELECT ... FOR UPDATE SKIP LOCKED`; single scheduler shown.

## Examples
Flag:
```python
@app.task
def send_reminders():                  # every 1 min, takes ~3 min, 2 beat instances
    for u in User.objects.filter(reminded=False): send(u); u.reminded = True; u.save()
```
OK:
```python
with advisory_lock("send_reminders") as got:
    if not got: return
```

## Evaluation cases
1. Above flag example -> flag.
2. Job with `SKIP LOCKED` claims -> no finding.
3. Daily job running seconds, single scheduler -> no finding.
4. Job with `soft_time_limit` that kills mid-loop leaving half-updated records and no resume logic -> flag.
