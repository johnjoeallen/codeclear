# java/sqs-visibility-timeout-and-delete-timing: SQS delete timing vs processing duration

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: AWS SDK |

## Intent
Deleting an SQS message before processing completes loses it on failure; processing longer than the visibility timeout makes the message visible again, causing duplicate concurrent processing.

## Why AI
It needs processing duration, the queue's visibility timeout and any extension logic, from code and configuration, and a concrete timing window.

## AI prompt
"Trace when `deleteMessage` happens relative to processing and when the visibility timeout expires (queue config or `visibilityTimeout` on receive, heartbeats via `changeMessageVisibility`). Report only a concrete window where a message is lost or processed concurrently twice, using the shown numbers." Evidence: poller/listener, queue attributes, worst-case processing time, DLQ/redrive policy.

## Do not flag
- Delete after successful processing with processing time well below the timeout.
- Spring Cloud AWS acknowledgement modes with default behaviour and no overrides.
- Timeout values not shown.

## Examples
Flag: `deleteMessage` called first, then a payment call that may throw.
OK: delete after commit; visibility extended during long tasks.

## Evaluation cases
1. Delete before processing - report.
2. 5-minute processing, 30s timeout, no extension - report.
3. Delete after success, 2s work, 30s timeout - no report.
4. Timeout unknown, delete after success - no report.
