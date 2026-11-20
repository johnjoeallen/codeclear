# Review rules

One folder per rule, with the Markdown rule file in it. Each file is the input for an AI generator that produces a
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

Each rule has its own folder, `<folder>/<rule-id>/`, with kebab-case rule ids unique within a language folder. The finding id is `<folder>/<rule-id>`.

```
review/java/empty-catch-block/
  rule.md         the rule (source of truth)
  rule.groovy     generated detector (executable rules only)
  tests.md        test cases as Markdown (executable rules only)
```

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

## Executable rules

Rules of type `ast`, `regex`, `linter` and `hybrid` run as Groovy scripts through the Java scripting
API (JSR-223), with a sandbox and a read-only `CodeModel`. Each has two files beside the rule:
`rule.groovy` (the detector) and `tests.md` (cases) in the rule's folder. `ai` rules have neither.
The tool regenerates a script automatically when its rule file changes (the script header holds a
`rule-hash`). See `docs/ai-generated-deterministic-checks.md#script-engine` and `#generation-workflow`.

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

### `tests.md` format

Test cases are Markdown so that a person can review them and an AI can write them. Each case is a heading, a
`covers:` line tying it to a section of `rule.md` (`example:flag`, `do-not-flag:<name>`, `detect:<name>`), and one
or more fenced source files. A line that must produce a finding ends in `// FLAG` (or `// FLAG LOW`); lines
without a marker must produce none. Markers are stripped before analysis.

````markdown
# Tests: java/empty-catch-block

## Prelude
```java
import java.util.*;
```

## Case: empty block (Examples: Flag)
covers: example:flag, required:empty-block
```java path=src/main/java/A.java
class A { void f() { try { run(); } catch (Exception e) { } } }  // FLAG
```

## Mutations
- `empty block (Examples: Flag)` -> `parameter named ignored (Examples: OK)`
````

Optional pieces: a `## Prelude` section (declarations placed before every source, not counted in line numbers);
a ```` ```java prelude ```` block inside a case to replace it; `config: key=value` lines for a project-wide case; several
`path=` blocks for a case spanning files; a `## Mutations` list of `flagged case` -> `fixed case` pairs.
