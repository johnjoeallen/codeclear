# java/mock-verifies-implementation-details: Mock verification instead of observable behaviour

| Field | Value |
|---|---|
| Applies to | `src/test/**/*.java` |
| Category | TESTING |
| Severity | LOW |
| Confidence | LOW |
| Detection type | ai |
| Source | java-review: Tests |

## Intent
Tests that mostly verify internal calls (`verify(repo).save(...)`, call counts, `verifyNoMoreInteractions`) break on refactoring and do not prove the outcome callers observe.

## Why AI
Whether a verification is the observable behaviour (a message sent, a side effect on a boundary collaborator) or an internal detail depends on the unit's contract.

## AI prompt
"Does this test assert the outcome visible to callers (return value, state, emitted event, call to an external boundary), or only how the unit calls its internal collaborators? Report only if the test would pass while the behaviour is wrong, or fail on a behaviour-preserving refactor; name that refactor or defect." Evidence: the test, the class under test, collaborators' roles.

## Do not flag
- Verification of calls to an external boundary that is the behaviour (email sender, publisher).
- Interaction tests for adapters whose job is delegation.

## Examples
Flag: test only `verify(mapper).toDto(entity); verify(repo).save(entity);` with no assertion on the result.
OK: `assertEquals("A-1", result.getCode()); verify(publisher).publish(any(OrderCreated.class));`.

## Evaluation cases
1. Only verifies private-helper collaborator order - report.
2. Verifies `emailSender.send(...)` as the feature - no report.
3. `verifyNoMoreInteractions` after asserting the result - no report.
4. Return value never asserted, mocks return stubs - report.
