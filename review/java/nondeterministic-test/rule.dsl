// generated-from: review/java/nondeterministic-test/rule.md
// rule-hash: 33866a1d4f1c5095a14c62e791d3faa31d6c4e9eca9f743ef6c796dea472ded5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'nondeterministic-test'

def clockType = { t -> t in ['LocalDate', 'LocalDateTime', 'LocalTime', 'Instant', 'ZonedDateTime', 'OffsetDateTime', 'Year', 'YearMonth'] }

// Whether the value is asserted on or only logged is not visible here, so this is a candidate for the AI.
Creation.each { x ->
    only file.isTest

    // "Do not flag": an injected Clock (now(clock) has an argument) and a seeded Random (new Random(42)).
    candidate x, "${x.scope.text}.now() reads the system clock: the result differs between runs, so inject a fixed Clock",
        x.name == 'now' && x.args.isEmpty() && clockType(x.scope.text)
    candidate x, "System.currentTimeMillis() reads the system clock: the result differs between runs",
        x.name in ['currentTimeMillis', 'nanoTime'] && x.scope.text == 'System'
    candidate x, "${x.typeName} with no arguments reads the system clock or an unseeded generator: the result differs between runs",
        x.typeName in ['Date', 'Random'] && x.args.isEmpty()
    candidate x, "${x.scope.text}.${x.name}() gives a different value on every run",
        (x.name == 'random' && x.scope.text == 'Math') || (x.name == 'randomUUID' && x.scope.text == 'UUID')
            || (x.name == 'current' && x.scope.text == 'ThreadLocalRandom')
    candidate x, "Thread.sleep() waits a fixed time for something else to finish: the test is slow and flaky, so wait for the condition",
        x.name == 'sleep' && x.scope.text == 'Thread'
    // "Do not flag": WireMock, Testcontainers and localhost; a real remote host is a network dependency.
    candidate x, "The test calls a real remote host: it fails when the network or the host is down",
        x.typeName == 'URL' && x.firstArg.text ==~ /"https?:\/\/.*/ && !(x.firstArg.text ==~ /"https?:\/\/(localhost|127\.0\.0\.1|\[::1\]).*/)
}
