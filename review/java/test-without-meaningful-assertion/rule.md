# java/test-without-meaningful-assertion: Test with no assertion or only assertNotNull

| Field | Value |
|---|---|
| Applies to | `src/test/**/*.java` |
| Category | TESTING |
| Severity | MEDIUM |
| Confidence | HIGH |
| Detection type | ast |
| Source | java-review: Tests |

## Intent
A test that asserts nothing, or only that something is not null, passes while the behaviour it names is broken.

## Detect
A method annotated `@Test`, `@ParameterizedTest` or `@RepeatedTest` whose body contains:
- no call to an assertion (`assert*`, `assertThat`, `fail`, `verify`, `then(...).should`, `StepVerifier.verify`, `mockMvc...andExpect`) and no `expected`/`assertThrows`; or
- assertions that are all `assertNotNull`/`assertThat(x).isNotNull()`.

## Do not flag
- Tests that only assert via `@Test(expected = ...)` or `assertThrows`/`assertDoesNotThrow`.
- Assertions delegated to a helper whose name starts with `assert`, `verify`, `check` or `expect`.
- Smoke tests named `contextLoads`.

## Examples
Flag:
```java
@Test void createsOrder() { assertNotNull(service.create(req)); }
```
OK:
```java
@Test void createsOrder() { assertEquals(Status.NEW, service.create(req).getStatus()); }
```

## Implementation notes
AST over test methods; resolve static-imported assertion names. Anchor to the method declaration.

## Required tests
No assertion (finding), only `assertNotNull` (finding), `assertThrows` (none), helper `verifyOrder(...)` (none), `contextLoads` (none).

## Why deterministic
Assertion calls and test annotations are syntactic; the exclusion list is closed.
