# common/commented-out-code: Block of disabled code

| Field | Value |
|---|---|
| Applies to | any source file |
| Category | COMMENT_QUALITY |
| Severity | INFO |
| Confidence | LOW |
| Detection type | hybrid |
| Source | comment-quality-review: Commented-out code |

## Intent
Disabled code with no explanation goes stale and makes readers unsure whether it should be restored.

## Detect
Three or more consecutive added comment lines in which at least 60 percent of lines look like code: end in `;`, `{`, `}` or `)`, contain `=` assignments, call syntax `name(...)`, or start with a keyword (`if`, `for`, `return`, `def`, `import`).

## Do not flag
- Comments that include an explanation line (`kept because`, `see`, `disabled until`).
- Documentation comments with code examples (inside `/** */` with `{@code}`, docstrings, or `@example`).
- License headers.

## Examples
Flag:
```java
// if (user == null) {
//     return;
// }
```
OK:
```java
// Example: if (user == null) return;
```

## Implementation notes
Heuristic, so keep `confidence: LOW` and report once per block. The AI reviewer can drop false positives.

Java files only, for now. A run of three or more line comments on consecutive lines, or a block comment of three or more lines, in which at least 60 percent of the non-blank lines look like code (end in `;`, `{` or `}`, contain an assignment or a call, or start with a keyword such as `if`, `for`, `return`). Javadoc, a run that mentions `copyright` or `license`, and a run that says why it is kept (`kept because`, `see`, `disabled until`, `todo`, `example`, `note:`) are skipped. Only the first comment of a run is reported.

## Required tests
Three code-like lines (finding), prose comment (no finding), docstring example (no finding), block with explanation (no finding).

## AI prompt
"Is this comment block disabled code rather than prose or an example, and is there no explanation of why it is kept?" Evidence: the comment block and three lines of surrounding code.

## Why hybrid
Code-likeness is a heuristic; the reviewer decides whether the block is truly disabled code.
