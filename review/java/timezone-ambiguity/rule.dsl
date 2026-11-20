// generated-from: review/java/timezone-ambiguity/rule.md
// rule-hash: 4c0406f21799d5bdc32f4f6ff6bdab84a85953d1108a33f5fd848dcf1c51686b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'timezone-ambiguity'

// Each of these takes the time zone of the machine the code runs on. Whether local time is what is meant is for the AI.
Call.each { c ->
    skip file.isTest

    candidate c, "ZoneId.systemDefault() depends on the machine's time zone: use an explicit zone such as ZoneOffset.UTC or an injected Clock",
        c.scope.text == 'ZoneId' && c.name == 'systemDefault'
    candidate c, "TimeZone.getDefault() depends on the machine's time zone: use an explicit zone",
        c.scope.text == 'TimeZone' && c.name == 'getDefault'
    // "Do not flag": now(ZoneOffset.UTC) and now(clock) have an argument, so only the argument-less call is a candidate.
    candidate c, "${c.scope.text}.now() reads the machine's clock and time zone: pass a Clock or a ZoneId",
        c.text in ['LocalDateTime.now()', 'LocalDate.now()']
}
