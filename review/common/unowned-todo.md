# common/unowned-todo: TODO, FIXME or HACK without a reference

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | COMMENT_QUALITY |
| Severity | INFO |
| Confidence | HIGH |
| Detection type | regex |
| Source | comment-quality-review: Unowned TODO/FIXME |

## Intent
A marker with no ticket or owner records a known gap that nobody is tracking.

## Detect
A comment added in the change containing `TODO`, `FIXME`, `HACK` or `XXX` as a whole word, with no ticket reference (`[A-Z][A-Z0-9]+-\d+`, `#\d+`, an issue URL) and no owner (`TODO(name)`).

## Do not flag
- Markers that already existed before the change (only changed lines).
- Markers with a ticket or owner.
- Text inside string literals.

## Examples
Flag:
```java
// TODO handle retries
```
OK:
```java
// TODO(PAY-1234): handle retries
```

## Implementation notes
Use a comment-aware tokenizer per language so strings are skipped. Always `INFO`.

## Required tests
Plain TODO (finding), with ticket (no finding), `TODO(jane)` (no finding), in a string literal (no finding), unchanged line (no finding).

## Why deterministic
A marker word in a comment on a changed line with no ticket or owner token is a lexical fact.
