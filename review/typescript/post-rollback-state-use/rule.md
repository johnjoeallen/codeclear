# typescript/post-rollback-state-use: Uncommitted entities, ids, events or cache used after rollback

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Databases, ORM, and transactions; node-bug-hunter: Database invariants and rollback |

## Intent
After a rollback or failed flush, mutated models, generated ids, cache entries and emitted events still exist in memory. A caller that treats them as persisted acts on state the database never saw.

## Why AI
Needs the caller's use of the returned object and the ordering of events/cache writes relative to commit.

## AI prompt
"After the failure or rollback, which in-memory entity, id, cache write or event is still used by a caller as if committed? Name the caller and the wrong outcome. Do not report speculative inconsistency without a caller." Evidence: transaction code, callers, event emitters, cache writes.

## Do not flag
- Events emitted after commit (outbox or `afterCommit` hook).
- Objects discarded on error.

## Examples
Flag:
```ts
try { await tx(async (t) => { user.credits -= 5; await save(user, t); throw e; }); } catch {} return user; // credits already reduced in memory
```
OK: events published in an `afterCommit` hook; model reloaded after failure.

## Evaluation cases
1. Cache set inside the transaction before commit: flag.
2. Event published after `await commit()`: ok.
3. Mutated entity returned from the catch path: flag.
