# java/nondeterministic-test: Test depends on time, randomness, order or the network

| Field | Value |
|---|---|
| Applies to | `src/test/**/*.java` |
| Category | TESTING |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-review: Tests |

## Intent
Tests that read the current time, use unseeded randomness, depend on execution order or call real networks fail intermittently and hide regressions.

## Detect
In a test class, any of:
- `LocalDate/LocalDateTime/Instant/ZonedDateTime.now()`, `System.currentTimeMillis()`, `new Date()` without an injected `Clock`.
- `new Random()`, `Math.random()`, `UUID.randomUUID()` used in an assertion's expected value, `ThreadLocalRandom`.
- `Thread.sleep` used for synchronisation.
- `@TestMethodOrder` or static mutable state shared between tests.
- `new URL("http...")`, `HttpClient`, `RestTemplate`, `Socket` calls to non-localhost hosts, not mocked.

## Do not flag
- Seeded `Random`, fixed `Clock`, WireMock/Testcontainers/localhost.
- Time used only for logging or unique names that are not asserted on.

## Examples
Flag:
```java
assertEquals(LocalDate.now().plusDays(1), svc.nextDay());
```
OK: `assertEquals(LocalDate.of(2024,1,2), new Svc(fixedClock).nextDay())`.

## Implementation notes
AST/regex candidates in `src/test`. Anchor to the call.

Detected in test sources by call and constructor name. `@TestMethodOrder`, static mutable state shared between tests, and whether the time or random value is asserted on or only logged are not checked: left to the AI.

## Required tests
`now()` in assertion (candidate), fixed clock (none), seeded Random (none), `Thread.sleep` (candidate), WireMock URL (none).

## Why hybrid
Usage of the clock or randomness can be harmless (not asserted on, or deliberately bounded).

## AI prompt
"Can the outcome of this test vary between runs because of time, randomness, ordering or external network? Report only with the specific varying input and how the assertion can fail." Evidence: the test, its fixtures and test configuration.
