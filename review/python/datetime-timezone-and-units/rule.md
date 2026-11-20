# python/datetime-timezone-and-units: Timezone, DST, timestamp-unit and serialization errors

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow |

## Intent
Comparing naive and aware datetimes, converting across DST/timezones incorrectly, mixing seconds and milliseconds, or serializing times inconsistently across services produces wrong instants. `naive-datetime` covers the deterministic constructor case.

## Why AI
Whether a wall-clock date is intended, which unit/zone a peer service expects, and the stored column semantics are contextual.

## AI prompt
"Does a changed datetime/timestamp operation mix naive and aware values, mis-handle DST or zone conversion, confuse seconds with milliseconds, or serialize in a format the shown consumer will misread?" Supply the code, producer and consumer formats and DB column types. Report only with a concrete trigger scenario (input instant, wrong outcome, changed line); otherwise NO_FINDING.

## Do not flag
- Deliberate local wall-clock dates (birthdays, business days) with no instant comparison.
- Values consistently UTC-aware end to end.

## Examples
Flag:
```python
expires = time.time() * 1000            # ms
if token.exp < expires: ...             # exp is in seconds
```
OK:
```python
expires = datetime.now(timezone.utc)
```

## Evaluation cases
1. `datetime.now() - aware_dt` -> flag (TypeError/mix).
2. `date + timedelta(days=1)` for a calendar-date rule -> no finding.
3. Local-time `replace(tzinfo=...)` across a DST change for a scheduled job -> flag.
4. `isoformat()` without offset sent to a service that parses as UTC while the producer is local -> flag.
