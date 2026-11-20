# typescript/duplicate-insert-and-affected-rows: App-only uniqueness check or ignored affected-row count

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | CONCURRENCY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: Databases, ORM, and transactions; node-bug-hunter: Database invariants and rollback |

## Intent
"Find then insert" lets concurrent requests both insert. Ignoring conflict errors, optimistic-lock failures or an update that affected zero rows lets the code report success for a write that did not happen.

## Why AI
Needs the schema's unique constraints and what the response or event claims after the write.

## AI prompt
"Is uniqueness enforced only by an application check? Is an affected-row count, upsert result or conflict error ignored while a response or event claims success? Give the concurrent request or the stale-version scenario." Evidence: queries, unique indexes/migrations, response mapping.

## Do not flag
- A unique constraint plus handled conflict error.
- Idempotent upserts where zero affected rows is acceptable.

## Examples
Flag:
```ts
if (!(await User.findOne({ email }))) await User.create({ email });
```
OK:
```ts
try { await User.create({ email }); } catch (e) { if (isUniqueViolation(e)) throw new Conflict(); throw e; }
```

## Evaluation cases
1. Find-then-create, no unique index in schema: flag.
2. Same code with unique index and conflict handling: ok.
3. `update(...)` result ignored, then 200 "updated": flag when a version guard is used.
