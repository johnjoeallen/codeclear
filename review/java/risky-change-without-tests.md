# java/risky-change-without-tests: Risky changed branch with no test in the MR

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | java-review: Tests; java-bug-hunter: Test evidence |

## Intent
New or changed business-logic branches, especially failure, redelivery, transaction-boundary or concurrency paths, should be exercised by a test in the same MR.

## Why AI
Which branches are risky, and whether a supplied test actually reaches them, needs understanding of the logic and the tests. A missing test is not proof of a defect and tests elsewhere must not be assumed absent.

## AI prompt
"List the risky behaviours added or changed in the production diff. For each, state whether a test in the supplied MR exercises it (including the failure path). Report only material, uncovered risk and name the behaviour; do not claim tests are missing elsewhere." Evidence: production diff, test files in the MR, MR description.

## Do not flag
- Trivial getters, mapping, logging or config-only changes.
- Behaviour covered by a test shown in the diff, even if in another class.
- Tests that may exist outside the MR.

## Examples
Flag: new retry-on-conflict branch in `PaymentService`, MR adds no test of the conflict path.
OK: new branch plus a test forcing the exception and asserting the retry.

## Evaluation cases
1. New error-compensation branch, no test files in MR - report.
2. Rename refactor, no tests - no report.
3. New branch covered by a test shown - no report.
4. Test touches the happy path only, new change is the redelivery guard - report.
