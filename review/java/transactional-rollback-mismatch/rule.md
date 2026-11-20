# java/transactional-rollback-mismatch: Transaction rollback does not match the failure handling

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: Transactions and persistence boundaries |

## Intent
A swallowed failure inside a `@Transactional` method commits partial work, and checked exceptions do not roll back by default, so the transaction commits although the operation failed.

## Detect
In a `@Transactional` method (or its class):
- a `catch` that does not rethrow and has writes before the `try`; or
- a `throws`/`throw` of a checked exception (`Exception`, `IOException`, custom checked) when the annotation has no `rollbackFor`.
- a catch that wraps a `RuntimeException` into a checked one.

## Do not flag
- `rollbackFor` (or `noRollbackFor`) that matches intent.
- `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` in the catch.
- Intentional `noRollbackFor` documented in a comment.

## Examples
Flag:
```java
@Transactional void run() throws IOException { repo.save(a); export(); }
```
OK: `@Transactional(rollbackFor = IOException.class)`.

## Implementation notes
AST on the method, resolving checked exception types. Anchor to the annotation or the catch.

Method-level `@Transactional` or a transactional class; `rollbackFor` or `noRollbackFor` anywhere in the annotation suppresses it. A write is a call to `save`, `persist`, `merge`, `delete`, `update`, `insert`, `flush` and similar; whether it happens before the `try` is not checked. A documented `noRollbackFor` comment is not read.

## Required tests
Checked throws with no rollbackFor (candidate), `rollbackFor` (none), swallowed catch after write (candidate), `setRollbackOnly` (none).

## Why hybrid
The syntax is deterministic; whether partial commit is the intended outcome needs intent.

## AI prompt
"After a failure on this path, does the transaction commit work that should have been rolled back? Name the failing statement and the persisted partial state; report only if one exists." Evidence: method, annotation attributes, callees that throw, transaction manager config.
