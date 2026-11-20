# java/kafka-rebalance-and-poll-interval: Processing time or local state breaks on rebalance

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Kafka |

## Intent
If per-record processing exceeds `max.poll.interval.ms` the consumer is evicted, triggering a rebalance and duplicate processing. Consumers holding partition-specific in-memory state lose or corrupt it when partitions move.

## Why AI
It needs worst-case processing time, poll limits and batch size from configuration, plus whether state is partition-bound.

## AI prompt
"Using `max.poll.records`, `max.poll.interval.ms` and the worst-case per-record work visible in the code (synchronous remote calls, retries, sleeps), report only if processing a full poll can exceed the interval, giving the numbers. For state kept per partition, report only if no `ConsumerRebalanceListener`/state reset exists and moved partitions would use stale state." Evidence: listener, consumer properties, any retry/backoff, in-memory maps keyed by partition or entity.

## Do not flag
- Short, bounded work well inside the poll interval.
- State that is reloaded from a durable store on every record.
- Cases where configuration is not shown.

## Examples
Flag: `max.poll.records=500`, each record makes a 5s HTTP call (2500s) with `max.poll.interval.ms=300000`.
OK: `max.poll.records=10` with 2s calls and a timeout.

## Evaluation cases
1. 500 records x 5s, interval 300s - report.
2. 10 records x 100ms - no report.
3. Local `Map<Partition, Offset>` cache with no rebalance listener, partition reassignment plausible - report.
4. No config shown - no report.
