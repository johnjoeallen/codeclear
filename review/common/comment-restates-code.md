# common/comment-restates-code: Comments that only repeat the code

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | COMMENT_QUALITY |
| Severity | INFO |
| Confidence | LOW |
| Detection type | hybrid |
| Source | comment-quality-review: What to check, Comments restating the code |

## Intent
Comments such as `// increment i` add noise without information. Reported once per file, and only when frequent in the changed code.

## Detect
In added lines, a single-line comment immediately before or after a statement where, after lowercasing and removing stop words, at least 80 percent of the comment tokens also appear as identifiers or keywords in the adjacent statement (for example `// increment counter` above `counter++`). Produce a candidate only when a file has three or more such comments.

## Do not flag
- Doc comments, license headers, generated banners, tool directives (`// eslint-disable`, `# noqa`, `//nolint`).
- Comments that add a reason, unit or constraint.
- Files with fewer than three candidates; report once per file, not per comment.

## Examples
Flag: three comments like `// set name` above `user.setName(name);`.
OK: `// normalise to UTC before comparing` above `t = t.toUtc();`.

## Implementation notes
Comment-aware tokenizer per language; anchor to the first candidate in the file. Report one finding per file.

## Required tests
Three restating comments (candidate), two (none), comment adding a reason (none), directive (none), doc comment (none).

## AI prompt
"Do these comments carry information beyond the next line, such as a reason, unit or constraint? Confirm only if the comments are purely redundant and frequent." Evidence: the candidate comments with their adjacent statements.

## Why hybrid
Token overlap finds likely repetition; whether a comment is informative is a judgement.
