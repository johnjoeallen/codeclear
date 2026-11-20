# typescript/invariant-bypass-across-entry-points: Business invariant enforced on one entry point only

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: HTTP frameworks and backend boundaries |

## Intent
A rule enforced in an HTTP handler (limits, state transitions, permissions) must also hold for scheduled jobs, workers, consumers and direct persistence paths, or those paths bypass it.

## Why AI
Requires identifying the invariant and every writer of the data across files.

## AI prompt
"Name the invariant. Find another entry point (cron, queue consumer, script, other route) that writes the same data without the check, and describe the bypass. Do not prescribe architectural layers." Evidence: the changed handler, other writers of the same entity (search results), the requirement.

## Do not flag
- Invariants enforced in a shared service or DB constraint used by all paths.

## Examples
Flag: the API rejects a transfer over limit, but the batch consumer calls `repo.transfer` directly.
OK: both call `TransferService.transfer`, which validates.

## Evaluation cases
1. Check only in controller; consumer calls repository: flag.
2. Check in DB constraint: ok.
3. Only one writer exists: ok.
