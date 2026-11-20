# python/unbounded-buffering: Unbounded result lists, subprocess output and queues

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Resources, deployment, and shutdown |

## Intent
Reading whole files/bodies/result sets/subprocess output into memory, or using unbounded queues, exhausts memory on realistic input; a full pipe or bounded queue with no consumer blocks the producer (`Popen` with unread `PIPE`).

## Why AI
Needs realistic input sizes and who consumes the buffer.

## AI prompt
"Can realistic input make this buffer grow without bound, or block a producer because nothing drains a pipe/queue?" Supply the code, the source and typical/maximum sizes, and the consumer. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Bounded sources (config, small lookups); streaming/chunked reads; `communicate()` on small output.

## Examples
Flag:
```python
p = subprocess.Popen(cmd, stdout=PIPE, stderr=PIPE); p.wait()      # pipe fills, deadlock
```
OK:
```python
out, err = p.communicate(timeout=60)
```

## Evaluation cases
1. Above flag example -> flag.
2. `data = await request.body()` for a file upload endpoint with no limit -> flag.
3. `rows = list(cursor)` on a 50-row config table -> no finding.
4. `queue.Queue()` fed by a fast producer with a slow consumer, no maxsize -> flag.
