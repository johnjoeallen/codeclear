# Review rules

One Markdown file per rule. Each file is the input for an AI generator that produces a
deterministic detector and its tests. See `docs/ai-generated-deterministic-checks.md`.

## Folders

| Folder | Applies to |
|---|---|
| `java` | `*.java` |
| `typescript` | `*.ts`, `*.tsx`, `*.js`, `*.jsx` |
| `python` | `*.py` |
| `sql` | `*.sql` |
| `config` | `*.yaml`, `*.yml`, `*.properties`, `*.json` |
| `common` | any source file, language-independent |

## Naming

`<rule-id>.md`, kebab-case, unique within a folder. The finding id is `<folder>/<rule-id>`.

## Template

```markdown
# <folder>/<rule-id>: <title>

| Field | Value |
|---|---|
| Applies to | file globs |
| Category | CORRECTNESS / ERROR_HANDLING / SECURITY / CONCURRENCY / TESTING / LOGGING / COMMENT_QUALITY / CONFIG |
| Severity | CRITICAL / HIGH / MEDIUM / LOW / INFO |
| Confidence | HIGH / MEDIUM / LOW |
| Detection type | ast / regex / linter / hybrid |
| Source | the original review instruction this rule comes from |

## Intent
The defect in one or two sentences.

## Detect
Precise, testable conditions.

## Do not flag
Exclusions that prevent false positives.

## Examples
Flag: code that must produce a finding.
OK: code that must not.

## Implementation notes
Preferred mechanism, existing linter equivalent, which line the finding anchors to.

## Required tests
The cases the generator must produce.
```

## Detection types

| Type | Meaning | AI call at review time |
|---|---|---|
| `ast`, `regex`, `linter` | Fully deterministic. A generated detector decides. | No |
| `hybrid` | A detector finds candidates and the AI reviewer confirms or drops them. | Yes, for candidates only |
| `ai` | Cannot be decided deterministically (needs intent, cross-file or runtime context). | Yes, always, using the rule's `AI prompt` |

Every rule file has an `## Why deterministic` or `## Why AI` section explaining the choice.
Rules of type `ai` and `hybrid` also have an `## AI prompt` section: the exact review question the
tool passes to the model, together with the evidence it must supply (diff, related files, ticket text).
Rules of type `ai` have no `## Required tests` for a detector; they have `## Evaluation cases`
(snippets with the expected verdict) used to check the prompt.

## Coverage

`INDEX.md` lists every rule, its detection type, and whether the tool makes an AI call for it.
All findings must anchor to a changed line.
