# typescript/changed-code-without-test: Changed component or logic has no matching test

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | typescript-review: Tests |

## Intent
Changed behavior in an area that normally has tests should come with a test.

## Why AI
Needs the repository's testing convention, the other changed files, and a judgement of whether the change is behavior or refactor. A file-name match cannot tell.

## AI prompt
"Does the project have tests for this area? Does this change alter behavior that those tests would cover, and is no test added or updated in the change? Report only if both are true and name the untested behavior." Evidence: diff, list of changed files, sibling test files for the module, test naming convention.

## Do not flag
- Projects or folders with no tests.
- Pure renames, types, comments, styling or config.
- Tests that exist outside the diff and clearly cover the change.

## Examples
Flag: a new branch in `calcDiscount.ts` while `calcDiscount.test.ts` exists and is untouched.
OK: the same change with a new test case; or a rename-only change.

## Evaluation cases
1. Logic change in tested module, no test edit: flag.
2. Logic change plus updated test: ok.
3. Edit in a folder with no test files: ok.
4. Prettier reformat only: ok.
